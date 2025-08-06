package com.sysu.smartjob.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCategoryVO {
    private String category;
    
    private List<String> levels;
    
    private Long jobCount;
}