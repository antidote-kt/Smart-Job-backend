package com.sysu.smartjob.constant;

public class PromptConstant {
    public static final String PROMPT = "为%s公司%s岗位生成一道面试题目，难度等级：%s，面试类型：%s。请只返回题目内容，不要包含其他文字。";
    
    public static final String IMPROVED_PROMPT =
            """
            为%s公司%s岗位生成第%d道面试题目，难度等级：%s，面试类型：%s。
            
            已提问题目：
            %s
            
            生成要求：
            1. 先搜索"%s岗位面试题"和"%s公司面试经验"，了解常见面试问题和真实面试情况
            2. 根据搜索结果和岗位特点生成针对性问题
            3. 避免与已提问题目重复或相似
            4. 题目要有实际考察价值，参考真实企业面试标准
            5. 难度等级：%s，确保问题深度合适
            6. **重要：必须使用中文生成面试题目**
            7. **关键：只返回纯粹的题目内容，不要包含任何标题、解答、说明文字或格式标记（如###、**等）**
            8. **不要包含"这个问题旨在考察"、"这道题属于"等解释性文字**
            
            请现在开始搜索相关面试信息，然后直接输出一道高质量的中文面试题目，不要任何其他内容。
            """;
    
    public static final String EVALUATE_PROMPT = """
    请评价以下面试回答：
    题目：%s
    回答：%s
    
    评估步骤：
    1. 先搜索这道题目的标准答案和优秀回答案例
    2. 对比候选人回答与标准答案的差异
    3. 从三个维度评分（每项1-100分）：
       - 专业技能评分：技术知识掌握程度，参考行业标准
       - 逻辑思维评分：思路清晰度和逻辑严密性
       - 回答完整性评分：回答是否全面完整
    
    请严格按照以下JSON格式返回评价结果：
    {
        "professionalScore": 85,
        "logicScore": 80,
        "completenessScore": 75,
        "overallScore": 80,
        "feedback": "基于搜索到的标准答案，提供具体的评价和改进建议"
    }
    """;

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
            
            请严格按照以下JSON格式返回报告内容：
            {
                "performanceAnalysis": "整体表现分析内容",
                "skillAssessment": "技能水平评估内容",
                "improvementSuggestions": "改进建议内容",
                "strongPoints": "突出优点内容",
                "weakPoints": "需要改进的方面内容"
            }
            """;
}
