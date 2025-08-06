package com.sysu.smartjob.constant;

public class PromptConstant {
    public static final String PROMPT = "为%s岗位生成一道面试题目，难度等级：%s，面试类型：%s。请只返回题目内容，不要包含其他文字。";
    
    public static final String EVALUATE_PROMPT = "请评价以下面试回答：\n题目：%s\n回答：%s\n\n请给出1-100的分数，并提供详细的评价反馈。格式：分数|评价内容";
    
    public static final String OVERALL_FEEDBACK_PROMPT = "面试结束，共回答%d道题目，平均得分%.2f分。请根据整体表现给出综合评价。";
    
    public static String getDifficultyText(Integer level) {
        switch (level) {
            case 1:
                return "简单";
            case 2:
                return "中等";
            case 3:
                return "困难";
            default:
                return "中等";
        }
    }
    
    public static String getInterviewTypeText(Integer type) {
        switch (type) {
            case 1:
                return "技术面试";
            case 2:
                return "HR面试";
            case 3:
                return "综合面试";
            default:
                return "技术面试";
        }
    }
}
