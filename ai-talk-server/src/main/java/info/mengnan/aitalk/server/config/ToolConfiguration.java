package info.mengnan.aitalk.server.config;

import info.mengnan.aitalk.rag.tools.Tools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ToolConfiguration {

    @Bean
    public Tools tools(){
        return new Tools();
    }
}
