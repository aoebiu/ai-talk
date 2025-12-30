package info.mengnan.aitalk.server.param.auth;

import lombok.Data;

@Data
public class MemberResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private String token;
}