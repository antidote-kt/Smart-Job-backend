package com.sysu.smartjob.service.impl;

import com.sysu.smartjob.dto.JobQueryDTO;
import com.sysu.smartjob.entity.JobRequirement;
import com.sysu.smartjob.mapper.JobRequirementMapper;
import com.sysu.smartjob.service.JobRequirementService;
import com.sysu.smartjob.vo.JobCategoryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JobRequirementServiceImpl implements JobRequirementService {

    @Autowired
    private JobRequirementMapper jobRequirementMapper;


    @Override
    public List<JobRequirement> getJobRequirementsByCondition(JobQueryDTO queryDTO) {
        JobRequirement query = JobRequirement.builder()
                .isActive(1)
                .build();
        BeanUtils.copyProperties(queryDTO, query);
        return jobRequirementMapper.findByCondition(query);
    }

    @Override
    public JobRequirement getJobRequirementById(Long id) {
        return jobRequirementMapper.findById(id);
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

    @Override
    public JobRequirement createJobRequirement(JobRequirement jobRequirement) {
        jobRequirement.setCreatedAt(LocalDateTime.now());
        jobRequirement.setUpdatedAt(LocalDateTime.now());
        jobRequirement.setIsActive(1);
        jobRequirementMapper.insert(jobRequirement);
        return jobRequirement;
    }

    @Override
    public JobRequirement updateJobRequirement(JobRequirement jobRequirement) {
        jobRequirement.setUpdatedAt(LocalDateTime.now());
        jobRequirementMapper.update(jobRequirement);
        return jobRequirement;
    }

    @Override
    public void deleteJobRequirement(Long id) {
        jobRequirementMapper.deleteById(id);
    }
}