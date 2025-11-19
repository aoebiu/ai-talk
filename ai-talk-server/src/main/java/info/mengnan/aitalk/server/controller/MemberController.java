package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.server.param.*;
import info.mengnan.aitalk.server.param.auth.*;
import info.mengnan.aitalk.server.service.MemberAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberAuthService memberAuthService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public R register(@RequestBody RegisterRequest request) {
        memberAuthService.register(
                request.getUsername(),
                request.getPassword(),
                request.getNickname(),
                request.getPhone()
        );
        return R.ok("注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R login(@RequestBody LoginRequest request) {
        MemberVO memberVO = memberAuthService.login(
                request.getUsername(),
                request.getPassword()
        );
        return R.ok(memberVO);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public R logout() {
        StpUtil.logout();
        return R.ok("退出成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public R info() {
        MemberVO memberVO = memberAuthService.getCurrentMemberInfo();
        return R.ok(memberVO);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public R update(@RequestBody UpdateMemberRequest request) {
        memberAuthService.updateMemberInfo(
                request.getNickname(),
                request.getPhone(),
                request.getAvatar()
        );
        return R.ok("更新成功");
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public R changePassword(@RequestBody ChangePasswordRequest request) {
        memberAuthService.changePassword(
                request.getOldPassword(),
                request.getNewPassword()
        );
        return R.ok("密码修改成功");

    }
}