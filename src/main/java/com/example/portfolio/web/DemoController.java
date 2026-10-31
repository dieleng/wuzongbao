package com.example.portfolio.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 托管解压后的在线体验包:/demo/{id}/** → uploads/demo/{id}/**
 * 访客在项目详情页通过 iframe 直接游玩
 */
@Controller
public class DemoController {

    /** MediaTypeFactory 未覆盖的补充类型 */
    private static final Map<String, MediaType> EXTRA_TYPES = Map.of(
            "wasm", MediaType.parseMediaType("application/wasm"));

    @Value("${app.upload-dir}")
    private String uploadDir;

    @GetMapping({"/demo/{id}", "/demo/{id}/{*path}"})
    public ResponseEntity<Resource> serve(@PathVariable Long id,
                                          @PathVariable(required = false) String path) {
        String rel = (path == null || path.isEmpty() || "/".equals(path))
                ? "index.html"
                : (path.startsWith("/") ? path.substring(1) : path);
        if (rel.isEmpty()) {
            rel = "index.html";
        }

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize()
                .resolve("demo").resolve(String.valueOf(id)).normalize();
        Path target = root.resolve(rel).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String name = target.getFileName().toString().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        MediaType mt = EXTRA_TYPES.get(ext);
        if (mt == null) {
            mt = MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM);
        }
        return ResponseEntity.ok().contentType(mt).body(new FileSystemResource(target));
    }
}
