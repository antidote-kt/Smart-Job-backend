package com.sysu.smartjob.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequirementVO {
    private Long id;
    
    private String name;
    
    private String category;
    
    private String level;
    
    private String description;
    
    private List<String> requirements;
    
    private List<String> skills;
    
    private LocalDateTime createdAt;
}