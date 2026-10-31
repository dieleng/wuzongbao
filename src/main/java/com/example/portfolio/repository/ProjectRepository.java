package com.example.portfolio.repository;

import com.example.portfolio.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** 前台:已发布,按权重降序 */
    List<Project> findAllByPublishedTrueOrderBySortWeightDescIdDesc();

    /** 后台:全部,按权重降序 */
    List<Project> findAllByOrderBySortWeightDescIdDesc();
}
