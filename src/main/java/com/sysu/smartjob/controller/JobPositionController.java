package com.sysu.smartjob.controller;

import com.sysu.smartjob.dto.JobQueryDTO;
import com.sysu.smartjob.entity.JobRequirement;
import com.sysu.smartjob.result.Result;
import com.sysu.smartjob.service.JobRequirementService;
import com.sysu.smartjob.vo.JobCategoryVO;
import com.sysu.smartjob.vo.JobRequirementVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/positions")
public class JobPositionController {

    @Autowired
    private JobRequirementService jobRequirementService;

    @GetMapping
    public Result<List<JobRequirementVO>> getPositions(JobQueryDTO queryDTO) {
        log.info("获取岗位列表，查询条件：{}", queryDTO);
        List<JobRequirement> jobRequirements = jobRequirementService.getJobRequirementsByCondition(
                queryDTO.getCategory(), queryDTO.getLevel(), queryDTO.getName());
        
        List<JobRequirementVO> vos = jobRequirements.stream()
                .map(job -> {
                    JobRequirementVO vo = new JobRequirementVO();
                    BeanUtils.copyProperties(job, vo);
                    return vo;
                })
                .collect(Collectors.toList());
        
        return Result.success(vos, "获取岗位列表成功");
    }

    @GetMapping("/{id}")
    public Result<JobRequirementVO> getPositionById(@PathVariable Long id) {
        log.info("获取岗位详情，岗位ID：{}", id);
        JobRequirement jobRequirement = jobRequirementService.getJobRequirementById(id);
        
        if (jobRequirement == null) {
            return Result.error("岗位不存在");
        }
        
        JobRequirementVO vo = new JobRequirementVO();
        BeanUtils.copyProperties(jobRequirement, vo);
        
        return Result.success(vo, "获取岗位详情成功");
    }

    @GetMapping("/categories")
    public Result<List<JobCategoryVO>> getCategories() {
        log.info("获取岗位分类列表");
        List<JobCategoryVO> categories = jobRequirementService.getJobCategories();
        return Result.success(categories, "获取岗位分类成功");
    }
}