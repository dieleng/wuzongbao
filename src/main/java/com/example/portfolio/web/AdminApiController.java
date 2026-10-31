package com.example.portfolio.web;

import com.example.portfolio.config.WebMvcConfig;
import com.example.portfolio.entity.AdminUser;
import com.example.portfolio.entity.Project;
import com.example.portfolio.repository.AdminUserRepository;
import com.example.portfolio.repository.ProjectRepository;
import com.example.portfolio.service.ProjectAssetsService;
import com.example.portfolio.web.dto.ProjectForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 后台 API:登录 / 项目 CRUD / 图片上传
 */
@RestController
@RequestMapping("/admin/api")
public class AdminApiController {

    /** 允许上传的图片后缀 */
    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");

    private final AdminUserRepository adminUserRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAssetsService assets;
    private final BCryptPasswordEncoder encoder;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public AdminApiController(AdminUserRepository adminUserRepository,
                              ProjectRepository projectRepository,
                              ProjectAssetsService assets,
                              BCryptPasswordEncoder encoder) {
        this.adminUserRepository = adminUserRepository;
        this.projectRepository = projectRepository;
        this.assets = assets;
        this.encoder = encoder;
    }

    // ---------- 登录 / 登出 ----------

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestParam String username,
                                                     @RequestParam String password,
                                                     HttpSession session) {
        AdminUser admin = adminUserRepository.findByUsername(username).orElse(null);
        if (admin == null || !encoder.matches(password, admin.getPasswordHash())) {
            return ResponseEntity.status(401)
                    .body(Map.of("ok", false, "message", "用户名或密码错误"));
        }
        session.setAttribute(WebMvcConfig.SESSION_KEY, admin.getUsername());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return Map.of("ok", true);
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpSession session) {
        return Map.of("ok", true, "username", String.valueOf(session.getAttribute(WebMvcConfig.SESSION_KEY)));
    }

    // ---------- 项目 CRUD ----------

    @PostMapping("/projects")
    public Map<String, Object> create(@Valid @RequestBody ProjectForm form) {
        Project saved = projectRepository.save(form.toEntity());
        return Map.of("ok", true, "id", saved.getId());
    }

    @PutMapping("/projects/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody ProjectForm form) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
        form.applyTo(p);
        projectRepository.save(p);
        return Map.of("ok", true, "id", p.getId());
    }

    @DeleteMapping("/projects/{id}")
    public Map<String, Object> delete(@PathVariable Long id) throws IOException {
        projectRepository.deleteById(id);
        // 同时清理该项目的体验包 / 源码包目录
        assets.deleteForProject(id);
        return Map.of("ok", true);
    }

    // ---------- 体验包 / 源码包上传 ----------

    @PostMapping("/projects/{id}/demo")
    public Map<String, Object> uploadDemo(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file) throws IOException {
        requireProject(id);
        assets.extract(id, "demo", file);
        return Map.of("ok", true);
    }

    @PostMapping("/projects/{id}/code")
    public Map<String, Object> uploadCode(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file) throws IOException {
        requireProject(id);
        assets.extract(id, "code", file);
        return Map.of("ok", true);
    }

    @DeleteMapping("/projects/{id}/demo")
    public Map<String, Object> deleteDemo(@PathVariable Long id) {
        requireProject(id);
        assets.deleteDemo(id);
        return Map.of("ok", true);
    }

    @DeleteMapping("/projects/{id}/code")
    public Map<String, Object> deleteCode(@PathVariable Long id) {
        requireProject(id);
        assets.deleteCode(id);
        return Map.of("ok", true);
    }

    private Project requireProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
    }

    // ---------- 图片上传 ----------

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片文件");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase();
        if (!IMAGE_EXTS.contains(ext)) {
            throw new IllegalArgumentException("只支持图片格式:" + String.join("/", IMAGE_EXTS));
        }

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = dir.resolve(filename);
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return Map.of("ok", true, "url", "/uploads/" + filename);
    }

    // ---------- 统一错误返回 ----------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数错误" : fe.getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("ok", false, "message", msg));
    }
}
