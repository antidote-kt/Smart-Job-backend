package com.sysu.smartjob.mapper;

import com.sysu.smartjob.entity.AnswerEvaluation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AnswerEvaluationMapper {
    
    int insert(AnswerEvaluation answerEvaluation);
    
    AnswerEvaluation findById(AnswerEvaluation answerEvaluation);
    
    List<AnswerEvaluation> findByCondition(AnswerEvaluation answerEvaluation);
    
    int update(AnswerEvaluation answerEvaluation);
    
    int deleteById(Long id);
}