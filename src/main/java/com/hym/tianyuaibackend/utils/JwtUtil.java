package com.hym.tianyuaibackend.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;

public class JwtUtil {

    private static final String SECRET = "tianyu-ai-admin-secret-key-2024";
    private static final long EXPIRE_TIME = 86400000L; // 24小时

    /**
     * 生成管理员JWT Token
     */
    public static String createToken(Long adminId, String username) {
        return JWT.create()
                .withClaim("adminId", adminId)
                .withClaim("username", username)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 验证Token并返回adminId
     */
    public static Long verifyToken(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(SECRET))
                    .build()
                    .verify(token)
                    .getClaim("adminId")
                    .asLong();
        } catch (Exception e) {
            return null;
        }
    }
}