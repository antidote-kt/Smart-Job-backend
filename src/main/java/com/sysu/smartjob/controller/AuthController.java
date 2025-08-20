package com.sysu.smartjob.controller;

import com.sysu.smartjob.constant.JwtClaimsConstant;
import com.sysu.smartjob.context.BaseContext;
import com.sysu.smartjob.dto.LoginDTO;
import com.sysu.smartjob.dto.PasswordDTO;
import com.sysu.smartjob.dto.UserDTO;
import com.sysu.smartjob.entity.User;
import com.sysu.smartjob.property.JwtProperties;
import com.sysu.smartjob.result.Result;
import com.sysu.smartjob.service.UserService;
import com.sysu.smartjob.utils.JwtUtil;
import com.sysu.smartjob.vo.LoginVO;
import com.sysu.smartjob.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserDTO userDTO) {
        log.info("注册：{}", userDTO);
        userService.register(userDTO);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        log.info("登录:{}", loginDTO);
        User user = userService.login(loginDTO.getUsername(), loginDTO.getPassword());

        LoginVO loginVO = new LoginVO();
        Map<String, Object> claim = new HashMap<String, Object>();
        claim.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claim);
        loginVO.setToken(token);
        loginVO.setUserId(user.getId());

        return Result.success(loginVO, "登录成功");
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("退出成功");
    }

    @GetMapping("/profile")
    public Result<UserVO> getProfile() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取用户信息：{}", userId);
        User user = userService.findById(userId);
        
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        return Result.success(userVO, "获取用户信息成功");
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UserDTO userDTO) {
        log.info("更新用户信息：{}", userDTO);
        userDTO.setUserId(BaseContext.getCurrentId());
        userService.updateProfile(userDTO);
        return Result.success("更新成功");
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody PasswordDTO passwordDTO) {
        log.info("修改密码请求");
        Long userId = BaseContext.getCurrentId();
        userService.changePassword(userId, passwordDTO.getOldPassword(), passwordDTO.getNewPassword());
        return Result.success("密码修改成功");
    }
}
