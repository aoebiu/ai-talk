package info.mengnan.aitalk.server.service;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.repository.entity.ChatMember;
import info.mengnan.aitalk.repository.service.MemberService;
import info.mengnan.aitalk.server.param.auth.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MemberAuthService {

    private final MemberService memberService;

    /**
     * 用户注册
     */
    public void register(String username, String password, String nickname, String phone) {
        if (memberService.findByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        if (StringUtils.hasText(phone) && memberService.findByPhone(phone) != null) {
            throw new IllegalArgumentException("手机号已被注册");
        }

        ChatMember chatMember = new ChatMember();
        chatMember.setUsername(username);
        chatMember.setPassword(encryptPassword(password));
        chatMember.setNickname(nickname);
        chatMember.setPhone(phone);
        chatMember.setStatus(1);

        memberService.insert(chatMember);
    }

    /**
     * 用户登录
     */
    public MemberVO login(String username, String password) {
        ChatMember chatMember = memberService.findByUsername(username);
        if (chatMember == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (!encryptPassword(password).equals(chatMember.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (chatMember.getStatus() != 1) {
            throw new IllegalStateException("用户已被禁用");
        }

        StpUtil.login(chatMember.getId());
        String token = StpUtil.getTokenValue();

        return buildMemberVO(chatMember, token);
    }

    /**
     * 获取当前登录用户信息
     */
    public MemberVO getCurrentMemberInfo() {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatMember chatMember = memberService.findById(memberId);
        if (chatMember == null) {
            throw new IllegalStateException("用户不存在");
        }
        return buildMemberVO(chatMember, null);
    }

    /**
     * 更新用户信息
     */
    public void updateMemberInfo(String nickname, String phone, String avatar) {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatMember chatMember = memberService.findById(memberId);
        if (chatMember == null) {
            throw new IllegalStateException("用户不存在");
        }

        // 更新允许修改的字段
        if (StringUtils.hasText(nickname)) {
            chatMember.setNickname(nickname);
        }
        if (StringUtils.hasText(phone)) {
            chatMember.setPhone(phone);
        }
        if (StringUtils.hasText(avatar)) {
            chatMember.setAvatar(avatar);
        }

        memberService.updateById(chatMember);
    }

    /**
     * 修改密码
     */
    public void changePassword(String oldPassword, String newPassword) {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatMember chatMember = memberService.findById(memberId);
        if (chatMember == null) {
            throw new IllegalStateException("用户不存在");
        }

        // 验证旧密码
        if (!encryptPassword(oldPassword).equals(chatMember.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }

        // 更新密码
        chatMember.setPassword(encryptPassword(newPassword));
        memberService.updateById(chatMember);
    }

    /**
     * 构建MemberVO对象
     */
    private MemberVO buildMemberVO(ChatMember chatMember, String token) {
        MemberVO vo = new MemberVO();
        vo.setId(chatMember.getId());
        vo.setUsername(chatMember.getUsername());
        vo.setNickname(chatMember.getNickname());
        vo.setPhone(chatMember.getPhone());
        vo.setAvatar(chatMember.getAvatar());
        vo.setStatus(chatMember.getStatus());
        vo.setToken(token);
        return vo;
    }

    /**
     * 密码加密
     */
    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
}