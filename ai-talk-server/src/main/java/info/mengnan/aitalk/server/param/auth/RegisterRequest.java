package info.mengnan.aitalk.server.param.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;
    private String phone;
}