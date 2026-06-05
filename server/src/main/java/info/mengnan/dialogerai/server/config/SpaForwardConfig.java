package info.mengnan.dialogerai.server.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardConfig {

    @GetMapping("/")
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
