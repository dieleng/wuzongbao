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

/**
 * 后台页面(数据由 AdminApiController 提供)
 */
@Controller
public class AdminPageController {

    private final ProjectRepository projectRepository;
    private final ProjectAssetsService assets;

    public AdminPageController(ProjectRepository projectRepository, ProjectAssetsService assets) {
        this.projectRepository = projectRepository;
        this.assets = assets;
    }

    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("projects", projectRepository.findAllByOrderBySortWeightDescIdDesc());
        return "admin/dashboard";
    }

    @GetMapping("/admin/projects/new")
    public String createPage(Model model) {
        Project p = new Project();
        p.setPublished(true);
        p.setSortWeight(0);
        model.addAttribute("p", p);
        return "admin/edit";
    }

    @GetMapping("/admin/projects/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("p", p);
        model.addAttribute("hasDemo", assets.hasDemo(p.getId()));
        model.addAttribute("hasCode", assets.hasCode(p.getId()));
        return "admin/edit";
    }
}
