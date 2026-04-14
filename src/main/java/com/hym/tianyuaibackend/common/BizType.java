package com.hym.tianyuaibackend.common;

/**
 * 文件上传业务类型枚举
 */
public enum BizType {
    AVATAR("avatar", "头像", "avatar/"),
    POST("post", "社区帖子", "community/"),
    NEWS("news", "资讯", "news/"),
    AI("ai", "AI诊断", "ai_detect/"),
    TEMP("temp", "临时文件", "temp/");

    private final String code;
    private final String description;
    private final String pathPrefix;

    BizType(String code, String description, String pathPrefix) {
        this.code = code;
        this.description = description;
        this.pathPrefix = pathPrefix;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    /**
     * 根据 code 获取枚举
     */
    public static BizType fromCode(String code) {
        for (BizType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return TEMP;
    }
}