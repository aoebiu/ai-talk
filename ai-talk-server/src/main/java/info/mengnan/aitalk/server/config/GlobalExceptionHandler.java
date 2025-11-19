package info.mengnan.aitalk.server.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.SaTokenException;
import info.mengnan.aitalk.server.param.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理Sa-Token未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public R handleNotLoginException(NotLoginException e) {
        log.warn("not Logged In Exception: {}", e.getMessage());
        return R.error("还未登录,请登录后重试");
    }

    /**
     * 处理Sa-Token其他异常
     */
    @ExceptionHandler(SaTokenException.class)
    public R handleSaTokenException(SaTokenException e) {
        log.error("Sa-Token异常: {}", e.getMessage());
        return R.error(e.getMessage());
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public R handleException(Exception e) {
        log.error("system Exception", e);
        return R.error("系统异常: " + e.getMessage());
    }
}