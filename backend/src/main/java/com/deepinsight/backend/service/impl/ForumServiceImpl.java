package com.deepinsight.backend.service.impl;

import com.deepinsight.backend.entity.ForumPost;
import com.deepinsight.backend.entity.ForumComment;
import com.deepinsight.backend.entity.User;
import com.deepinsight.backend.repository.ForumPostRepository;
import com.deepinsight.backend.repository.ForumCommentRepository;
import com.deepinsight.backend.repository.UserRepository;
import com.deepinsight.backend.service.ForumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class ForumServiceImpl implements ForumService {
    private final ForumPostRepository postRepo;
    private final ForumCommentRepository commentRepo;
    private final UserRepository userRepo;

    @Override public ForumPost createPost(Long userId, String title, String content, String coverUrl, boolean isOfficial) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        return postRepo.save(ForumPost.builder().userId(userId).title(title).content(content).coverUrl(coverUrl).isOfficial(isOfficial).build());
    }

    @Override public ForumPost updatePost(Long postId, Long userId, String title, String content) {
        ForumPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("帖子不存在"));
        if (Boolean.TRUE.equals(post.getIsOfficial())) throw new RuntimeException("官方帖子只能在管理后台维护");
        if (!post.getUserId().equals(userId)) throw new RuntimeException("无权编辑");
        if (title != null) post.setTitle(title);
        if (content != null) post.setContent(content);
        return postRepo.save(post);
    }

    @Override @Transactional
    public void deletePost(Long postId, Long userId) {
        ForumPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("帖子不存在"));
        User user = userRepo.findById(userId).orElseThrow();
        if (Boolean.TRUE.equals(post.getIsOfficial())) throw new RuntimeException("官方帖子只能在管理后台维护");
        if (!post.getUserId().equals(userId) && user.getRole() != User.Role.ADMIN) throw new RuntimeException("无权删除");
        commentRepo.deleteByPostId(postId);
        postRepo.delete(post);
    }

    @Override public ForumPost getPost(Long postId) {
        ForumPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("帖子不存在"));
        post.setViewCount(post.getViewCount() + 1);
        return postRepo.save(post);
    }

    @Override public List<ForumPost> listPosts() { return postRepo.findAllByOrderByIsPinnedDescCreatedAtDesc(); }
    @Override public List<ForumPost> listUserPosts(Long userId) { return postRepo.findByUserIdOrderByCreatedAtDesc(userId); }

    @Override public ForumComment addComment(Long postId, Long userId, String content, Long parentId) {
        if (!postRepo.existsById(postId)) throw new RuntimeException("帖子不存在");
        return commentRepo.save(ForumComment.builder().postId(postId).userId(userId).content(content).parentId(parentId).build());
    }

    @Override public List<ForumComment> getComments(Long postId) { return commentRepo.findByPostIdOrderByCreatedAtAsc(postId); }

    @Override public void deleteComment(Long commentId, Long userId) {
        ForumComment c = commentRepo.findById(commentId).orElseThrow(() -> new RuntimeException("评论不存在"));
        User user = userRepo.findById(userId).orElseThrow();
        if (!c.getUserId().equals(userId) && user.getRole() != User.Role.ADMIN) throw new RuntimeException("无权删除");
        commentRepo.delete(c);
    }
}
