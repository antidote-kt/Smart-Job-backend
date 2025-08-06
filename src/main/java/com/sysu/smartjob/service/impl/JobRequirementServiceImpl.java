package com.sysu.smartjob.service.impl;

import com.sysu.smartjob.entity.JobRequirement;
import com.sysu.smartjob.mapper.JobRequirementMapper;
import com.sysu.smartjob.service.JobRequirementService;
import com.sysu.smartjob.vo.JobCategoryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JobRequirementServiceImpl implements JobRequirementService {

    @Autowired
    private JobRequirementMapper jobRequirementMapper;

    @Override
    public List<JobRequirement> getAllJobRequirements() {
        JobRequirement query = JobRequirement.builder()
                .isActive(1)
                .build();
        return jobRequirementMapper.findByCondition(query);
    }

    @Override
    public List<JobRequirement> getJobRequirementsByCondition(String category, String level, String name) {
        JobRequirement query = JobRequirement.builder()
                .category(category)
                .level(level)
                .name(name)
                .isActive(1)
                .build();
        return jobRequirementMapper.findByCondition(query);
    }

    @Override
    public JobRequirement getJobRequirementById(Long id) {
        JobRequirement query = JobRequirement.builder()
                .id(id)
                .build();
        return jobRequirementMapper.findById(query);
    }

    @Override
    public List<JobCategoryVO> getJobCategories() {
        List<String> categories = jobRequirementMapper.findAllCategories();
        
        return categories.stream()
                .map(category -> {
                    List<String> levels = jobRequirementMapper.findLevelsByCategory(category);
                    Long jobCount = jobRequirementMapper.countByCategory(category);
                    
                    return JobCategoryVO.builder()
                            .category(category)
                            .levels(levels)
                            .jobCount(jobCount)
                            .build();
                })
                .collect(Collectors.toList());
    }
}