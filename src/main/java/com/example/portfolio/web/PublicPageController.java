package com.example.portfolio.web;

import com.example.portfolio.entity.Project;
import com.example.portfolio.repository.ProjectRepository;
import com.example.portfolio.service.ProjectAssetsService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 前台页面
 */
@Controller
public class PublicPageController {

    private final ProjectRepository projectRepository;
    private final ProjectAssetsService assets;

    public PublicPageController(ProjectRepository projectRepository, ProjectAssetsService assets) {
        this.projectRepository = projectRepository;
        this.assets = assets;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Project> projects = projectRepository.findAllByPublishedTrueOrderBySortWeightDescIdDesc();
        model.addAttribute("projects", projects);
        return "index";
    }

    @GetMapping("/project/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Project p = projectRepository.findById(id)
                .filter(Project::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("p", p);
        model.addAttribute("hasDemo", assets.hasDemo(p.getId()));
        model.addAttribute("hasCode", assets.hasCode(p.getId()));
        return "project";
    }
}
