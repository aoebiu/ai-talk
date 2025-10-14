package info.mengnan.aitalk.server.param.common;

import lombok.Data;

@Data
public class R {

    private boolean success;
    private String message;
    private Object data;

    public static R ok() {
        R r = new R();
        r.setSuccess(true);
        r.setMessage("success");
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
        r.setMessage("error");
        return r;
    }

    public static R error(String message) {
        R r = error();
        r.setMessage(message);
        return r;
    }
}
