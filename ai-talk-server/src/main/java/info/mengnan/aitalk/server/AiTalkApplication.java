package info.mengnan.aitalk.server;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@SpringBootApplication
@MapperScan("info.mengnan.aitalk.repository.mapper")
@ComponentScan(basePackages = {"info.mengnan.aitalk"})
public class AiTalkApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext context = SpringApplication.run(AiTalkApplication.class, args);
        Environment env = context.getEnvironment();

        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");
        String appName = env.getProperty("spring.application.name", "ai-talk");
        String profile = env.getActiveProfiles().length > 0 ?
                String.join(",", env.getActiveProfiles()) : "default";
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String localUrl = String.format("http://localhost:%s", port);
        String networkUrl = String.format("http://%s:%s", ip, port);
        log.info("""
                        
                        -----------------------------------------------\
                        
                        application     {}
                        profile         {}
                        local           {}
                        network         {}
                        port            {}
                        pid             {}
                        start_time      {}
                        -----------------------------------------------""",
                appName, profile, localUrl, networkUrl, port, pid, startTime);
    }
}
