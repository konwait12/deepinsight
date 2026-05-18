package com.deepinsight.backend.service;

import com.deepinsight.backend.entity.ForumPost;
import com.deepinsight.backend.entity.ForumComment;
import java.util.List;

public interface ForumService {
    // 帖子
    ForumPost createPost(Long userId, String title, String content, String coverUrl, boolean isOfficial);
    ForumPost updatePost(Long postId, Long userId, String title, String content);
    void deletePost(Long postId, Long userId);
    ForumPost getPost(Long postId);
    List<ForumPost> listPosts();
    List<ForumPost> listUserPosts(Long userId);

    // 评论
    ForumComment addComment(Long postId, Long userId, String content, Long parentId);
    List<ForumComment> getComments(Long postId);
    void deleteComment(Long commentId, Long userId);
}
