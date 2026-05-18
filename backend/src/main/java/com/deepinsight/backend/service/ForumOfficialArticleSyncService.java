package com.deepinsight.backend.service;

import com.deepinsight.backend.entity.ForumPost;
import com.deepinsight.backend.entity.KnowledgeArticle;
import com.deepinsight.backend.entity.ModelArticle;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.ForumPostRepository;
import com.deepinsight.backend.repository.KnowledgeArticleRepository;
import com.deepinsight.backend.repository.ModelArticleRepository;
import com.deepinsight.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ForumOfficialArticleSyncService {

    private static final String KNOWLEDGE_SOURCE = "KNOWLEDGE_ARTICLE";
    private static final String MODEL_SOURCE = "MODEL_ARTICLE";
    private static final String OFFICIAL_USERNAME = "deepinsight-official";
    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");

    private final ForumPostRepository forumPostRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final ModelArticleRepository modelArticleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SyncResult syncOfficialArticlesToForum() {
        Long officialUserId = resolveOfficialUserId();
        int created = 0;
        int updated = 0;

        for (KnowledgeArticle article : knowledgeArticleRepository.findAllByOrderByIsPinnedDescCreatedAtDesc()) {
            boolean wasCreated = upsertPost(
                KNOWLEDGE_SOURCE,
                article.getId(),
                article.getTitle(),
                article.getContent(),
                extractCoverUrl(article.getContent()),
                Boolean.TRUE.equals(article.getIsPinned()),
                officialUserId
            );
            if (wasCreated) created++; else updated++;
        }

        for (ModelArticle article : modelArticleRepository.findAll()) {
            boolean wasCreated = upsertPost(
                MODEL_SOURCE,
                article.getId(),
                article.getTitle(),
                article.getContent(),
                extractCoverUrl(article.getContent()),
                true,
                officialUserId
            );
            if (wasCreated) created++; else updated++;
        }

        return new SyncResult(created, updated, created + updated);
    }

    private boolean upsertPost(
        String sourceType,
        Long sourceId,
        String title,
        String content,
        String coverUrl,
        boolean pinned,
        Long officialUserId
    ) {
        ForumPost post = forumPostRepository.findBySourceTypeAndSourceIdAndIsOfficialTrue(sourceType, sourceId)
            .or(() -> forumPostRepository.findFirstByTitleAndIsOfficialTrueOrderByCreatedAtAsc(title))
            .orElse(null);

        boolean created = post == null;
        if (created) {
            post = ForumPost.builder()
                .viewCount(0)
                .isLocked(false)
                .build();
        }

        post.setTitle(title);
        post.setContent(content);
        post.setCoverUrl(coverUrl);
        post.setUserId(officialUserId);
        post.setIsOfficial(true);
        post.setIsPinned(pinned);
        post.setSourceType(sourceType);
        post.setSourceId(sourceId);
        forumPostRepository.save(post);
        return created;
    }

    private String extractCoverUrl(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        Matcher matcher = MARKDOWN_IMAGE.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Long resolveOfficialUserId() {
        return userRepository.findByUsername(OFFICIAL_USERNAME)
            .map(User::getId)
            .orElseGet(() -> userRepository.findByRole(User.Role.ADMIN).stream()
                .findFirst()
                .map(User::getId)
                .orElseGet(() -> userRepository.findAll().stream()
                    .findFirst()
                    .map(User::getId)
                    .orElseGet(this::createOfficialUser)));
    }

    private Long createOfficialUser() {
        User user = User.builder()
            .username(OFFICIAL_USERNAME)
            .email("official@deepinsight.local")
            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
            .role(User.Role.ADMIN)
            .build();
        return userRepository.save(user).getId();
    }

    public record SyncResult(int created, int updated, int total) {}
}
