package com.example.portfolio.web;

import com.example.portfolio.entity.Project;
import com.example.portfolio.repository.ProjectRepository;
import com.example.portfolio.service.ProjectAssetsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 公开源码浏览 API:文件树 + 单文件文本预览(仅已发布项目)
 */
@RestController
@RequestMapping("/api/projects/{id}")
public class CodeApiController {

    private final ProjectRepository projectRepository;
    private final ProjectAssetsService assets;

    public CodeApiController(ProjectRepository projectRepository, ProjectAssetsService assets) {
        this.projectRepository = projectRepository;
        this.assets = assets;
    }

    @GetMapping("/code")
    public List<Map<String, Object>> tree(@PathVariable Long id) throws IOException {
        requirePublished(id);
        return assets.tree(id);
    }

    @GetMapping("/code/file")
    public Map<String, Object> file(@PathVariable Long id, @RequestParam String path) throws IOException {
        requirePublished(id);
        return assets.readTextFile(id, path);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIo(IOException e) {
        return ResponseEntity.internalServerError().body(Map.of("ok", false, "message", "读取失败"));
    }

    private void requirePublished(Long id) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!p.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
