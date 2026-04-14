package com.hym.tianyuaibackend.service;

import java.util.Map;

/**
 * 资讯互动服务
 */
public interface INewsInteractionService {

    /**
     * 点赞/取消点赞
     *
     * @param userId 用户ID
     * @param newsId 资讯ID
     * @return 操作结果
     */
    Map<String, Object> toggleLike(Long userId, Long newsId);

    /**
     * 收藏/取消收藏
     *
     * @param userId 用户ID
     * @param newsId 资讯ID
     * @return 操作结果
     */
    Map<String, Object> toggleFavorite(Long userId, Long newsId);

    /**
     * 获取用户对资讯的互动状态
     *
     * @param userId 用户ID
     * @param newsId 资讯ID
     * @return 互动状态
     */
    Map<String, Object> getInteractionStatus(Long userId, Long newsId);

    /**
     * 发表评论
     *
     * @param userId 用户ID
     * @param newsId 资讯ID
     * @param content 评论内容
     * @return 操作结果
     */
    Map<String, Object> addComment(Long userId, Long newsId, String content);

    /**
     * 获取资讯评论列表
     *
     * @param newsId 资讯ID
     * @param page 页码
     * @param size 每页数量
     * @return 评论列表
     */
    Map<String, Object> getComments(Long newsId, Integer page, Integer size);

    /**
     * 删除评论
     *
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 操作结果
     */
    Map<String, Object> deleteComment(Long userId, Long commentId);
}
