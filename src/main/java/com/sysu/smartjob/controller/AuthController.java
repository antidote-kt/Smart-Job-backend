package com.sysu.smartjob.controller;

import com.sysu.smartjob.constant.JwtClaimsConstant;
import com.sysu.smartjob.dto.LoginDTO;
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
    public Result<UserVO> register(@RequestBody UserDTO userDTO) {
        User user = userService.register(userDTO);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        return Result.success(userVO, "注册成功");
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
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
    public Result<UserVO> getProfile(@RequestParam Long userId) {
        User user = userService.findById(userId);
        
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        return Result.success(userVO, "获取用户信息成功");
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestParam Long userId,
                                      @RequestParam String nickname,
                                      @RequestParam String email) {
        User user = userService.updateProfile(userId, nickname, email);
        
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        return Result.success(userVO, "更新成功");
    }
}
