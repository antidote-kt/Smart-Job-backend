package com.sysu.smartjob.service;

import com.sysu.smartjob.dto.UserDTO;
import com.sysu.smartjob.entity.User;

public interface UserService {

    void register(UserDTO userDTO);

    User login(String username, String password);
    
    User findById(Long userId);
    
    void updateProfile(UserDTO userDTO);
    
    void changePassword(Long userId, String oldPassword, String newPassword);


}