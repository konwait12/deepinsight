package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.ModelArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ModelArticleRepository extends JpaRepository<ModelArticle, Long> {
    Optional<ModelArticle> findByModelId(Long modelId);
    List<ModelArticle> findByTitle(String title);
}
