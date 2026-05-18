package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {
    List<KnowledgeArticle> findAllByOrderByIsPinnedDescCreatedAtDesc();
    List<KnowledgeArticle> findByNodeIdOrderByCreatedAtDesc(String nodeId);
    List<KnowledgeArticle> findByTitle(String title);
}
