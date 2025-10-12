package info.mengnan.aitalk.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("info.mengnan.aitalk.repository.mapper")
@ComponentScan(basePackages = {"info.mengnan.aitalk.*"})
public class AiTalkApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTalkApplication.class, args);
    }

}
