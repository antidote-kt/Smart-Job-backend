package com.sysu.smartjob.dto;

import lombok.Data;
import java.util.List;

@Data
public class JobRequirementCreateDTO {
    private String name;
    private String category;
    private String level;
    private String description;
    private List<String> requirements;
    private List<String> skills;
}