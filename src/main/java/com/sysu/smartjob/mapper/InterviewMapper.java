package com.sysu.smartjob.mapper;

import com.sysu.smartjob.entity.Interview;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface InterviewMapper {
    
    void insert(Interview interview);
    
    void update(Interview interview);
    
    Interview findById(Interview interview);
    
    List<Interview> findByCondition(Interview interview);
    
    void deleteById(Long id);
}