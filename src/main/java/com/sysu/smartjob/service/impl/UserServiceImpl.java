package com.sysu.smartjob.service.impl;

import com.sysu.smartjob.dto.UserDTO;
import com.sysu.smartjob.entity.User;
import com.sysu.smartjob.exception.UserNotLoginException;
import com.sysu.smartjob.mapper.UserMapper;
import com.sysu.smartjob.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void register(UserDTO userDTO) {
        // 检查用户名是否存在
        User existingUser = new User();
        existingUser.setUsername(userDTO.getUsername());
        User found = userMapper.select(existingUser);
        if (found != null) {
            throw new UserNotLoginException("用户名已存在");
        }

        // 检查邮箱是否存在
        existingUser = new User();
        existingUser.setEmail(userDTO.getEmail());
        found = userMapper.select(existingUser);
        if (found != null) {
            throw new UserNotLoginException("邮箱已存在");
        }

        // 创建新用户
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(DigestUtils.md5DigestAsHex(userDTO.getPassword().getBytes()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setStatus(1);

        int result = userMapper.insert(user);
        if (result > 0) {
            return;
        }
        throw new UserNotLoginException("注册失败");
    }

    @Override
    public User login(String username, String password) {
        User queryUser = new User();
        queryUser.setUsername(username);
        queryUser.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        
        User user = userMapper.select(queryUser);
        if (user == null) {
            throw new UserNotLoginException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new UserNotLoginException("账户已被禁用");
        }
        return user;
    }

    @Override
    public User findById(Long userId) {
        User queryUser = new User();
        queryUser.setId(userId);
        User user = userMapper.select(queryUser);
        if (user == null) {
            throw new UserNotLoginException("用户不存在");
        }
        return user;
    }

    @Override
    public void updateProfile(UserDTO userDTO) {
        User existingUser = findById(userDTO.getUserId());
        
        if (!existingUser.getEmail().equals(userDTO.getEmail())) {
            User emailCheck = new User();
            emailCheck.setEmail(userDTO.getEmail());
            User found = userMapper.select(emailCheck);
            if (found != null && !found.getId().equals(userDTO.getUserId())) {
                throw new UserNotLoginException("邮箱已被其他用户使用");
            }
        }

        existingUser.setNickname(userDTO.getNickname());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.update(existingUser);
        if (result > 0) {
            return;
        }
        throw new UserNotLoginException("更新失败");
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User existingUser = findById(userId);
        
        // 验证旧密码
        String oldPasswordHash = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        if (!existingUser.getPassword().equals(oldPasswordHash)) {
            throw new UserNotLoginException("旧密码错误");
        }
        
        // 更新密码
        existingUser.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        int result = userMapper.update(existingUser);
        if (result <= 0) {
            throw new UserNotLoginException("密码修改失败");
        }
    }
}