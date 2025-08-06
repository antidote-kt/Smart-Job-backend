package com.sysu.smartjob.mapper;

import com.sysu.smartjob.entity.JobRequirement;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface JobRequirementMapper {
    
    void insert(JobRequirement jobRequirement);
    
    void update(JobRequirement jobRequirement);
    
    JobRequirement findById(JobRequirement jobRequirement);
    
    List<JobRequirement> findByCondition(JobRequirement jobRequirement);
    
    List<String> findAllCategories();
    
    List<String> findLevelsByCategory(String category);
    
    Long countByCategory(String category);
    
    void deleteById(JobRequirement jobRequirement);
}