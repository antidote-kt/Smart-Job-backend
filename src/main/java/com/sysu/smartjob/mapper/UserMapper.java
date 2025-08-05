package com.sysu.smartjob.mapper;

import com.sysu.smartjob.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    
    int insert(User user);
    
    User select(User user);
    
    int update(User user);
    
    int delete(User user);
}