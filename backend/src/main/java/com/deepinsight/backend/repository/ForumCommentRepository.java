package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.ForumComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {
    List<ForumComment> findByPostIdOrderByCreatedAtAsc(Long postId);
    void deleteByPostId(Long postId);
}
