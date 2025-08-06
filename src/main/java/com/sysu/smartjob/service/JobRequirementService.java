package com.sysu.smartjob.service;

import com.sysu.smartjob.entity.JobRequirement;
import com.sysu.smartjob.vo.JobCategoryVO;

import java.util.List;

public interface JobRequirementService {
    
    List<JobRequirement> getAllJobRequirements();
    
    List<JobRequirement> getJobRequirementsByCondition(String category, String level, String name);
    
    JobRequirement getJobRequirementById(Long id);
    
    List<JobCategoryVO> getJobCategories();
}