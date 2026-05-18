package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.KnowledgeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNode, String> {
    List<KnowledgeNode> findByParentIdOrderBySortOrder(String parentId);
    List<KnowledgeNode> findByParentIdIsNullOrderBySortOrder();
}
