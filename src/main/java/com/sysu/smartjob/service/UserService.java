package com.sysu.smartjob.service;

import com.sysu.smartjob.dto.UserDTO;
import com.sysu.smartjob.entity.User;

public interface UserService {

    User register(UserDTO userDTO);

    User login(String username, String password);
    
    User findById(Long userId);
    
    User updateProfile(UserDTO userDTO);


}