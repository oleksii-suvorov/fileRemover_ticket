package oleksii.ticket.moveFiles.config;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:inputData.properties")
@ComponentScan("oleksii.ticket.moveFiles")
public class SpringConfig {
}
