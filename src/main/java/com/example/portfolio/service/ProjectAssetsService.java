package com.example.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 项目资源管理:在线体验包(demo zip)与源码包(code zip)的解压、浏览、删除
 */
@Service
public class ProjectAssetsService {

    /** 解压后总量上限 */
    private static final long MAX_TOTAL = 50L * 1024 * 1024;
    /** 解压后单文件上限 */
    private static final long MAX_FILE = 20L * 1024 * 1024;
    /** 解压后文件数上限 */
    private static final int MAX_COUNT = 500;
    /** 源码在线预览的大小上限 */
    private static final long MAX_PREVIEW = 200L * 1024;

    /** 允许在线预览源码的文本后缀 */
    private static final Set<String> TEXT_EXTS = Set.of(
            "java", "xml", "html", "htm", "css", "js", "ts", "json", "md", "txt",
            "sql", "yml", "yaml", "properties", "py", "c", "cpp", "h", "hpp",
            "cs", "go", "vue", "sh", "kt", "kts", "gradle", "bat", "csv",
            "ini", "conf", "toml", "php", "rb", "rs", "scala", "swift");

    private final Path baseDir;

    public ProjectAssetsService(@Value("${app.upload-dir}") String uploadDir) {
        this.baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path demoDir(Long id) {
        return baseDir.resolve("demo").resolve(String.valueOf(id));
    }

    public Path codeDir(Long id) {
        return baseDir.resolve("code").resolve(String.valueOf(id));
    }

    /** 是否已上传体验包(以根目录 index.html 为准) */
    public boolean hasDemo(Long id) {
        return Files.isRegularFile(demoDir(id).resolve("index.html"));
    }

    /** 是否已上传源码包 */
    public boolean hasCode(Long id) {
        return hasAnyFile(codeDir(id));
    }

    /**
     * 上传并解压 zip(type = demo / code),会覆盖旧版本
     */
    public void extract(Long id, String type, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择 zip 文件");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!original.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("只支持 .zip 文件");
        }

        Path dest = "demo".equals(type) ? demoDir(id) : codeDir(id);
        deleteRecursively(dest);
        Files.createDirectories(dest);

        // multipart 先落到临时文件,便于 ZipFile 以不同编码打开重试
        Path tmp = Files.createTempFile("portfolio-zip-", ".zip");
        try {
            file.transferTo(tmp);
            ZipFile opened;
            try {
                opened = new ZipFile(tmp.toFile(), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                // 中文文件名以 GBK 压缩的情况,换 GBK 编码重试
                opened = new ZipFile(tmp.toFile(), Charset.forName("GBK"));
            }
            try (ZipFile zf = opened) {
                extractAll(zf, dest);
            } catch (IOException | IllegalArgumentException e) {
                deleteRecursively(dest);
                if (e instanceof IllegalArgumentException iae) {
                    throw iae;
                }
                throw (IOException) e;
            }
        } finally {
            Files.deleteIfExists(tmp);
        }

        promoteRoot(dest);
        if (!hasAnyFile(dest)) {
            deleteRecursively(dest);
            throw new IllegalArgumentException("zip 包内容为空");
        }
    }

    private void extractAll(ZipFile zf, Path dest) throws IOException {
        long total = 0;
        int count = 0;
        Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            String name = e.getName();
            // Windows 打包工具(如 PowerShell Compress-Archive)用反斜杠分隔路径,统一为正斜杠
            if (name.indexOf('\\') >= 0) {
                name = name.replace('\\', '/');
            }
            // 跳过 macOS 打包产生的垃圾文件
            if (name.startsWith("__MACOSX") || name.contains("/__MACOSX/") || name.endsWith(".DS_Store")) {
                continue;
            }
            if (e.isDirectory() || name.endsWith("/")) {
                continue;
            }
            count++;
            if (count > MAX_COUNT) {
                throw new IllegalArgumentException("zip 内文件过多(超过 " + MAX_COUNT + " 个)");
            }

            Path target = dest.resolve(name).normalize();
            if (!target.startsWith(dest)) {
                throw new IllegalArgumentException("zip 内包含非法路径:" + name);
            }
            Files.createDirectories(target.getParent());

            try (InputStream in = zf.getInputStream(e);
                 OutputStream out = Files.newOutputStream(target)) {
                byte[] buf = new byte[8192];
                int n;
                long size = 0;
                while ((n = in.read(buf)) > 0) {
                    size += n;
                    total += n;
                    if (size > MAX_FILE) {
                        throw new IllegalArgumentException("zip 内单个文件超过 20MB:" + name);
                    }
                    if (total > MAX_TOTAL) {
                        throw new IllegalArgumentException("zip 解压后总量超过 50MB");
                    }
                    out.write(buf, 0, n);
                }
            }
        }
    }

    /** 若根目录没有 index.html 且只包了一层目录,把该目录内容上移 */
    private void promoteRoot(Path dest) throws IOException {
        int guard = 0;
        while (!Files.isRegularFile(dest.resolve("index.html")) && guard++ < 5) {
            List<Path> children;
            try (Stream<Path> s = Files.list(dest)) {
                children = s.toList();
            }
            if (children.size() != 1 || !Files.isDirectory(children.get(0))) {
                return;
            }
            Path only = children.get(0);
            List<Path> inner;
            try (Stream<Path> s = Files.list(only)) {
                inner = s.toList();
            }
            boolean movedAll = true;
            for (Path child : inner) {
                try {
                    Files.move(child, dest.resolve(child.getFileName().toString()),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    movedAll = false;
                    break;
                }
            }
            if (!movedAll) {
                return;
            }
            Files.deleteIfExists(only);
        }
    }

    private boolean hasAnyFile(Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> s = Files.walk(dir)) {
            return s.anyMatch(Files::isRegularFile);
        } catch (IOException e) {
            return false;
        }
    }

    /** 源码文件树(目录在前,同级按名称排序) */
    public List<Map<String, Object>> tree(Long id) throws IOException {
        Path root = codeDir(id);
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        return buildTree(root, root);
    }

    private List<Map<String, Object>> buildTree(Path root, Path dir) throws IOException {
        List<Path> children;
        try (Stream<Path> s = Files.list(dir)) {
            children = new ArrayList<>(s.toList());
        }
        children.sort(Comparator
                .comparing((Path p) -> !Files.isDirectory(p))
                .thenComparing(p -> p.getFileName().toString().toLowerCase()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Path p : children) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", p.getFileName().toString());
            m.put("path", root.relativize(p).toString().replace('\\', '/'));
            boolean isDir = Files.isDirectory(p);
            m.put("dir", isDir);
            if (isDir) {
                m.put("children", buildTree(root, p));
            }
            result.add(m);
        }
        return result;
    }

    /** 读取源码文本用于预览(UTF-8 优先,失败回退 GBK) */
    public Map<String, Object> readTextFile(Long id, String relPath) throws IOException {
        if (relPath == null || relPath.isBlank()) {
            throw new IllegalArgumentException("缺少文件路径");
        }
        Path root = codeDir(id).toAbsolutePath().normalize();
        Path target = root.resolve(relPath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        if (!Files.isRegularFile(target)) {
            throw new IllegalArgumentException("文件不存在");
        }
        String name = target.getFileName().toString();
        String ext = extOf(name);
        if (!TEXT_EXTS.contains(ext)) {
            throw new IllegalArgumentException("该文件类型不支持在线预览");
        }
        if (Files.size(target) > MAX_PREVIEW) {
            throw new IllegalArgumentException("文件超过 200KB,不支持在线预览");
        }
        byte[] bytes = Files.readAllBytes(target);
        return Map.of("ok", true, "name", name, "content", decode(bytes));
    }

    private String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("GBK"));
        }
    }

    private String extOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    public void deleteDemo(Long id) {
        deleteRecursively(demoDir(id));
    }

    public void deleteCode(Long id) {
        deleteRecursively(codeDir(id));
    }

    public void deleteForProject(Long id) {
        deleteDemo(id);
        deleteCode(id);
    }

    private void deleteRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
