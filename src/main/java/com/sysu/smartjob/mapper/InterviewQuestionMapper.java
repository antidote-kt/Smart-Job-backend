package com.sysu.smartjob.mapper;

import com.sysu.smartjob.entity.InterviewQuestion;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface InterviewQuestionMapper {
    
    void insert(InterviewQuestion question);
    
    void update(InterviewQuestion question);
    
    InterviewQuestion findById(InterviewQuestion question);
    
    List<InterviewQuestion> findByCondition(InterviewQuestion question);
    
    void deleteById(Long id);
}