package com.sysu.smartjob.constant;

public class PromptConstant {
    public static final String PROMPT = "为%s公司%s岗位生成一道面试题目，难度等级：%s，面试类型：%s。请只返回题目内容，不要包含其他文字。";
    
    public static final String EVALUATE_PROMPT = "请从以下三个维度评价以下面试回答：\n题目：%s\n回答：%s\n\n评分维度（每项1-100分）：\n1. 专业技能评分 - 考察专业知识和技能的掌握程度\n2. 逻辑思维评分 - 考察思路清晰度和逻辑严密性\n3. 回答完整性评分 - 考察回答是否全面、完整\n\n请按以下格式返回：\n专业技能分数,逻辑思维分数,回答完整性分数|综合评价内容";

    public static String getDifficultyText(Integer level) {
        return switch (level) {
            case 2 -> "中等";
            case 3 -> "困难";
            default -> "简单";
        };
    }
    
    public static String getInterviewTypeText(Integer type) {
        return switch (type) {
            case 2 -> "HR面试";
            case 3 -> "综合面试";
            default -> "技术面试";
        };
    }
    
    public static final String INTERVIEW_REPORT_PROMPT = """
            请为面试会话ID %d生成详细的面试报告。
            综合评分：%.2f
            专业技能评分：%.2f
            逻辑思维评分：%.2f
            回答完整性评分：%.2f
            
            请提供：
            1. 整体表现分析
            2. 技能水平评估
            3. 改进建议
            4. 突出优点
            5. 需要改进的方面
            
            请按以下格式返回：
            表现分析:[内容]
            技能评估:[内容]
            改进建议:[内容]
            突出优点:[内容]
            改进方面:[内容]
            """;
            
    // 报告解析标签常量
    public static final String REPORT_LABEL_PERFORMANCE = "表现分析";
    public static final String REPORT_LABEL_SKILL = "技能评估";
    public static final String REPORT_LABEL_IMPROVEMENT = "改进建议";
    public static final String REPORT_LABEL_STRENGTHS = "突出优点";
    public static final String REPORT_LABEL_WEAKNESSES = "改进方面";
    
    // 报告默认值常量
    public static final String DEFAULT_PERFORMANCE_ANALYSIS = "面试者整体表现良好，展现了扎实的专业基础和积极的学习态度。在回答问题时思路清晰，能够结合自身经验进行阐述，具备良好的沟通能力。";
    public static final String DEFAULT_SKILL_ASSESSMENT = "技术基础较为扎实，能够掌握岗位所需的核心技能。在解决问题时表现出一定的逻辑思维能力，但在复杂场景的应用上还有提升空间。";
    public static final String DEFAULT_IMPROVEMENT_SUGGESTIONS = "建议加强实际项目经验的积累，深入理解业务场景。可以通过参与开源项目、技术分享等方式提升综合能力。同时建议关注行业最新技术动态，保持技术敏感度。";
    public static final String DEFAULT_STRONG_POINTS = "学习能力强，能够快速掌握新知识；具备良好的团队协作精神；工作态度积极主动；基础知识掌握扎实。";
    public static final String DEFAULT_WEAK_POINTS = "实际项目经验相对不足；在复杂问题的分析上需要加强；对业务场景的理解需要进一步深化；技术深度有待提升。";
}
