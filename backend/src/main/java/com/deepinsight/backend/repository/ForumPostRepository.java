package com.deepinsight.backend.repository;

import com.deepinsight.backend.entity.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    List<ForumPost> findAllByOrderByIsPinnedDescCreatedAtDesc();
    List<ForumPost> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ForumPost> findBySourceTypeAndSourceIdAndIsOfficialTrue(String sourceType, Long sourceId);
    Optional<ForumPost> findFirstByTitleAndIsOfficialTrueOrderByCreatedAtAsc(String title);
}
