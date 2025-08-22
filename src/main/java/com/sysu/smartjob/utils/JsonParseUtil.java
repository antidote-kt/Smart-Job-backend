package com.sysu.smartjob.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysu.smartjob.constant.DefaultValueConstant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonParseUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 解析AI评估响应的JSON
     */
    public static class EvaluationResult {
        public double professionalScore = DefaultValueConstant.DEFAULT_SCORE;
        public double logicScore = DefaultValueConstant.DEFAULT_SCORE;
        public double completenessScore = DefaultValueConstant.DEFAULT_SCORE;
        public double overallScore = DefaultValueConstant.DEFAULT_SCORE;
        public String feedback = DefaultValueConstant.DEFAULT_FEEDBACK;
    }
    
    /**
     * 解析AI报告响应的JSON
     */
    public static class ReportResult {
        public String performanceAnalysis = DefaultValueConstant.DEFAULT_PERFORMANCE_ANALYSIS;
        public String skillAssessment = DefaultValueConstant.DEFAULT_SKILL_ASSESSMENT;
        public String improvementSuggestions = DefaultValueConstant.DEFAULT_IMPROVEMENT_SUGGESTIONS;
        public String strongPoints = DefaultValueConstant.DEFAULT_STRONG_POINTS;
        public String weakPoints = DefaultValueConstant.DEFAULT_WEAK_POINTS;
    }
    
    /**
     * 解析评估响应JSON
     */
    public static EvaluationResult parseEvaluation(String aiResponse) {
        EvaluationResult result = new EvaluationResult();
        
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            log.warn("AI评估响应为空");
            return result;
        }
        
        try {
            // 提取JSON部分
            String jsonContent = extractJsonFromResponse(aiResponse);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            if (node.has(DefaultValueConstant.JSON_PROFESSIONAL_SCORE)) {
                result.professionalScore = node.get(DefaultValueConstant.JSON_PROFESSIONAL_SCORE).asDouble(DefaultValueConstant.DEFAULT_SCORE);
            }
            if (node.has(DefaultValueConstant.JSON_LOGIC_SCORE)) {
                result.logicScore = node.get(DefaultValueConstant.JSON_LOGIC_SCORE).asDouble(DefaultValueConstant.DEFAULT_SCORE);
            }
            if (node.has(DefaultValueConstant.JSON_COMPLETENESS_SCORE)) {
                result.completenessScore = node.get(DefaultValueConstant.JSON_COMPLETENESS_SCORE).asDouble(DefaultValueConstant.DEFAULT_SCORE);
            }
            if (node.has(DefaultValueConstant.JSON_OVERALL_SCORE)) {
                result.overallScore = node.get(DefaultValueConstant.JSON_OVERALL_SCORE).asDouble(DefaultValueConstant.DEFAULT_SCORE);
            } else {
                // 如果没有总分，则计算平均值
                result.overallScore = (result.professionalScore + result.logicScore + result.completenessScore) / 3.0;
            }
            if (node.has(DefaultValueConstant.JSON_FEEDBACK)) {
                result.feedback = node.get(DefaultValueConstant.JSON_FEEDBACK).asText(DefaultValueConstant.DEFAULT_FEEDBACK);
            }
            
            log.info("成功解析AI评估结果: 专业{}, 逻辑{}, 完整{}, 总分{}", 
                    result.professionalScore, result.logicScore, result.completenessScore, result.overallScore);
                    
        } catch (Exception e) {
            log.warn("解析AI评估响应失败，使用默认值: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 解析报告响应JSON
     */
    public static ReportResult parseReport(String aiResponse) {
        ReportResult result = new ReportResult();
        
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            log.warn("AI报告响应为空");
            return result;
        }
        
        try {
            // 提取JSON部分
            String jsonContent = extractJsonFromResponse(aiResponse);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            if (node.has(DefaultValueConstant.JSON_PERFORMANCE_ANALYSIS)) {
                result.performanceAnalysis = node.get(DefaultValueConstant.JSON_PERFORMANCE_ANALYSIS).asText(result.performanceAnalysis);
            }
            if (node.has(DefaultValueConstant.JSON_SKILL_ASSESSMENT)) {
                result.skillAssessment = node.get(DefaultValueConstant.JSON_SKILL_ASSESSMENT).asText(result.skillAssessment);
            }
            if (node.has(DefaultValueConstant.JSON_IMPROVEMENT_SUGGESTIONS)) {
                result.improvementSuggestions = node.get(DefaultValueConstant.JSON_IMPROVEMENT_SUGGESTIONS).asText(result.improvementSuggestions);
            }
            if (node.has(DefaultValueConstant.JSON_STRONG_POINTS)) {
                result.strongPoints = node.get(DefaultValueConstant.JSON_STRONG_POINTS).asText(result.strongPoints);
            }
            if (node.has(DefaultValueConstant.JSON_WEAK_POINTS)) {
                result.weakPoints = node.get(DefaultValueConstant.JSON_WEAK_POINTS).asText(result.weakPoints);
            }
            
            log.info("成功解析AI报告结果");
            
        } catch (Exception e) {
            log.warn("解析AI报告响应失败，使用默认值: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 从响应中提取JSON内容
     */
    private static String extractJsonFromResponse(String response) {
        // 查找第一个 { 和最后一个 }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        
        // 如果没找到完整的JSON，返回原内容让后续处理
        return response;
    }
    
}