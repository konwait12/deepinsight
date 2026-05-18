package com.deepinsight.backend.repository;
import com.deepinsight.backend.entity.KnowledgeDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface KnowledgeDocRepository extends JpaRepository<KnowledgeDoc, Long> {
    List<KnowledgeDoc> findByCategory(String category);
    @Query(value = "SELECT * FROM knowledge_docs WHERE MATCH(title, content) AGAINST(?1 IN NATURAL LANGUAGE MODE) LIMIT 5", nativeQuery = true)
    List<KnowledgeDoc> searchByFulltext(String query);
}
