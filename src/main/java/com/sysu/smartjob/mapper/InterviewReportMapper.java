package com.sysu.smartjob.mapper;

import com.sysu.smartjob.entity.InterviewReport;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface InterviewReportMapper {
    
    int insert(InterviewReport interviewReport);
    
    InterviewReport select(InterviewReport interviewReport);
    
    InterviewReport findById(InterviewReport interviewReport);
    
    List<InterviewReport> findByCondition(InterviewReport interviewReport);
    
    int update(InterviewReport interviewReport);
    
    int deleteById(Long id);
}