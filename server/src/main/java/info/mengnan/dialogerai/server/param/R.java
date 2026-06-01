package info.mengnan.dialogerai.server.param;

import lombok.Data;

@Data
public class R {

    private boolean success;
    private String code;
    private String message;
    private Object data;

    public static R ok() {
        R r = new R();
        r.setSuccess(true);
        return r;
    }

    public static R ok(Object data) {
        R r = ok();
        r.setData(data);
        return r;
    }

    public static R ok(String message, Object data) {
        R r = ok();
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static R error() {
        R r = new R();
        r.setSuccess(false);
        return r;
    }

    public static R error(String message) {
        R r = error();
        r.setMessage(message);
        return r;
    }

    public static R error(ErrorCode errorCode) {
        R r = error();
        r.setCode(errorCode.getCode());
        r.setMessage(errorCode.getMessage());
        return r;
    }

    public static R unauthorized() {
        return error("unauthorized");
    }
}
