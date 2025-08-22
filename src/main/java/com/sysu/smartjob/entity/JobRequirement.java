package com.sysu.smartjob.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequirement {
    private Long id;
    
    private String name;
    
    private String category;
    
    private String level;
    
    private String description;
    
    // 数据库中存储的JSON字符串
    private String requirements;
    
    // 数据库中存储的JSON字符串
    private String skills;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private Integer isActive;
    
    @JsonIgnore
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 获取需求列表（转换JSON为List）
    public List<String> getRequirementsList() {
        return parseJsonToList(requirements);
    }
    
    // 设置需求列表（转换List为JSON）
    public void setRequirementsList(List<String> requirementsList) {
        this.requirements = parseListToJson(requirementsList);
    }
    
    // 获取技能列表（转换JSON为List）
    public List<String> getSkillsList() {
        return parseJsonToList(skills);
    }
    
    // 设置技能列表（转换List为JSON）
    public void setSkillsList(List<String> skillsList) {
        this.skills = parseListToJson(skillsList);
    }
    
    private List<String> parseJsonToList(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty() || "null".equals(jsonString)) {
            return null;
        }
        try {
            List<String> list = objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});
            List<String> filteredList = list.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.toList());
            return filteredList.isEmpty() ? null : filteredList;
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    private String parseListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<String> filteredList = list.stream()
            .filter(s -> s != null && !s.trim().isEmpty())
            .collect(Collectors.toList());
        if (filteredList.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(filteredList);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}