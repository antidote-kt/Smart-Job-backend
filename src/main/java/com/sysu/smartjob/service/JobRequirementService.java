package com.sysu.smartjob.service;

import com.sysu.smartjob.dto.JobQueryDTO;
import com.sysu.smartjob.entity.JobRequirement;
import com.sysu.smartjob.vo.JobCategoryVO;

import java.util.List;

public interface JobRequirementService {

    List<JobRequirement> getJobRequirementsByCondition(JobQueryDTO queryDTO);
    
    JobRequirement getJobRequirementById(Long id);
    
    List<JobCategoryVO> getJobCategories();
    
    JobRequirement createJobRequirement(JobRequirement jobRequirement);
    
    JobRequirement updateJobRequirement(JobRequirement jobRequirement);
    
    void deleteJobRequirement(Long id);
}