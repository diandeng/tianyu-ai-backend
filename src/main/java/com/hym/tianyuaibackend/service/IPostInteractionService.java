package com.hym.tianyuaibackend.service;

import java.util.Map;

/**
 * 帖子互动服务
 */
public interface IPostInteractionService {

    /**
     * 点赞/取消点赞
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 操作结果
     */
    Map<String, Object> toggleLike(Long userId, Long postId);

    /**
     * 收藏/取消收藏
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 操作结果
     */
    Map<String, Object> toggleFavorite(Long userId, Long postId);

    /**
     * 获取用户对帖子的互动状态
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 互动状态
     */
    Map<String, Object> getInteractionStatus(Long userId, Long postId);

    /**
     * 发表评论
     *
     * @param userId  用户ID
     * @param postId  帖子ID
     * @param content 评论内容
     * @param parentId 父评论ID（回复时使用）
     * @return 操作结果
     */
    Map<String, Object> addComment(Long userId, Long postId, String content, Long parentId);

    /**
     * 获取帖子评论列表
     *
     * @param postId 帖子ID
     * @param page   页码
     * @param size   每页数量
     * @return 评论列表
     */
    Map<String, Object> getComments(Long postId, Integer page, Integer size);

    /**
     * 删除评论
     *
     * @param userId    用户ID
     * @param commentId 评论ID
     * @return 操作结果
     */
    Map<String, Object> deleteComment(Long userId, Long commentId);
}