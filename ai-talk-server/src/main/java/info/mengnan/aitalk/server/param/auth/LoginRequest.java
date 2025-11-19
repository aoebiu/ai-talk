package info.mengnan.aitalk.server.param.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}