package com.sysu.smartjob.constant;

/**
 * Redis Key常量类
 */
public class RedisKeyConstant {
    
    /**
     * 生成用户面试缓存Key
     * @param userId 用户ID
     * @param interviewId 面试ID
     * @return 缓存Key: user:{userId}:interviews:{interviewId}
     */
    public static String getUserInterviewKey(Long userId, Long interviewId) {
        return "user:" + userId + ":interviews:" + interviewId;
    }
    
    /**
     * 生成用户面试Pattern（用于批量查询用户所有面试）
     * @param userId 用户ID
     * @return Pattern: user:{userId}:interviews:*
     */
    public static String getUserInterviewsPattern(Long userId) {
        return "user:" + userId + ":interviews:*";
    }
    
    /**
     * 生成面试题目列表Key
     * @param userId 用户ID
     * @param interviewId 面试ID
     * @return 缓存Key: user:{userId}:interviews:{interviewId}:questions
     */
    public static String getInterviewQuestionsKey(Long userId, Long interviewId) {
        return "user:" + userId + ":interviews:" + interviewId + ":questions";
    }
}