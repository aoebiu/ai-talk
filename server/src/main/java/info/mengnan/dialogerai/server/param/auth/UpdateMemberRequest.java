package info.mengnan.dialogerai.server.param.auth;

import lombok.Data;

@Data
public class UpdateMemberRequest {
    private String nickname;
    private String phone;
    private String avatar;
}