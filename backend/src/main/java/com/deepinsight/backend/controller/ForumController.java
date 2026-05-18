package com.deepinsight.backend.controller;

import com.deepinsight.backend.common.Result;
import com.deepinsight.backend.entity.*;
import com.deepinsight.backend.service.ForumOfficialArticleSyncService;
import com.deepinsight.backend.service.ForumService;
import com.deepinsight.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

/** 论坛 + 知识库公开接口 */
@RestController
@RequestMapping("/api/v1/forum")
public class ForumController {

    @Autowired private ForumService forumService;
    @Autowired private UserRepository userRepo;
    @Autowired private KnowledgeNodeRepository nodeRepo;
    @Autowired private KnowledgeArticleRepository articleRepo;
    @Autowired private ForumOfficialArticleSyncService officialArticleSyncService;

    // === 帖子 ===
    @PostMapping("/posts")
    public Result<ForumPost> create(@RequestBody PostReq req, Principal principal) {
        Long uid = getUserId(principal);
        if (uid == null) return Result.error(401, "请先登录");
        return Result.success(forumService.createPost(uid, req.title, req.content, req.coverUrl, false));
    }

    @GetMapping("/posts")
    public Result<List<ForumPost>> list() { return Result.success(forumService.listPosts()); }

    @PostMapping("/posts/sync-official")
    public Result<ForumOfficialArticleSyncService.SyncResult> syncOfficialPosts(Principal principal) {
        Long uid = getUserId(principal);
        if (uid == null) return Result.error(401, "Please login");
        User user = userRepo.findById(uid).orElse(null);
        if (user == null || user.getRole() != User.Role.ADMIN) return Result.error(403, "Admin required");
        return Result.success("Official articles synced to forum database", officialArticleSyncService.syncOfficialArticlesToForum());
    }

    @GetMapping("/posts/{id}")
    public Result<ForumPost> detail(@PathVariable Long id) { return Result.success(forumService.getPost(id)); }

    @PutMapping("/posts/{id}")
    public Result<ForumPost> update(@PathVariable Long id, @RequestBody PostReq req, Principal principal) {
        Long uid = getUserId(principal);
        if (uid == null) return Result.error(401, "请先登录");
        return Result.success(forumService.updatePost(id, uid, req.title, req.content));
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> delete(@PathVariable Long id, Principal principal) {
        Long uid = getUserId(principal);
        if (uid == null) return Result.error(401, "请先登录");
        forumService.deletePost(id, uid);
        return Result.success(null);
    }

    // === 评论 ===
    @PostMapping("/posts/{postId}/comments")
    public Result<ForumComment> addComment(@PathVariable Long postId, @RequestBody CommentReq req, Principal principal) {
        Long uid = getUserId(principal);
        if (uid == null) return Result.error(401, "请先登录");
        return Result.success(forumService.addComment(postId, uid, req.content, req.parentId));
    }

    @GetMapping("/posts/{postId}/comments")
    public Result<List<ForumComment>> comments(@PathVariable Long postId) {
        return Result.success(forumService.getComments(postId));
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id, Principal principal) {
        Long uid = getUserId(principal);
        if (uid == null) return Result.error(401, "请先登录");
        forumService.deleteComment(id, uid);
        return Result.success(null);
    }

    // === 知识库公开接口 ===
    @GetMapping("/knowledge/nodes")
    public Result<List<KnowledgeNode>> listNodes() { return Result.success(nodeRepo.findAll()); }

    @GetMapping("/knowledge/articles")
    public Result<List<KnowledgeArticle>> listArticles() {
        return Result.success(articleRepo.findAllByOrderByIsPinnedDescCreatedAtDesc());
    }

    @GetMapping("/knowledge/articles/{id}")
    public Result<KnowledgeArticle> getArticle(@PathVariable Long id) {
        KnowledgeArticle a = articleRepo.findById(id).orElseThrow(() -> new RuntimeException("文章不存在"));
        a.setViewCount(a.getViewCount() + 1);
        return Result.success(articleRepo.save(a));
    }

    @PostMapping("/knowledge/articles")
    public Result<KnowledgeArticle> createArticle(@RequestBody KnowledgeArticle article, Principal principal) {
        article.setId(null);
        Long uid = getUserId(principal);
        if (uid == null) return Result.error(401, "请先登录");
        if (article.getAuthorId() == null) {
            article.setAuthorId(uid);
        }
        return Result.success("文章创建成功", articleRepo.save(article));
    }

    private Long getUserId(Principal principal) {
        if (principal == null) return null;
        return userRepo.findByUsername(principal.getName()).map(com.deepinsight.backend.entity.User::getId).orElse(null);
    }
}

class PostReq { public String title; public String content; public String coverUrl; }
class CommentReq { public String content; public Long parentId; }
