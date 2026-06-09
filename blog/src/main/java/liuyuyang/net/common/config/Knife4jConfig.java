package liuyuyang.net.common.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@Configuration
@EnableKnife4j
@EnableSwagger2WebMvc
public class Knife4jConfig {

    @Bean
    public Docket thriveXApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("thrivex")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("liuyuyang.net.web.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("ThriveX API")
                .description("Stable REST API for ThriveX Blog, Admin, and Server.")
                .version("v4.0")
                .contact(new Contact("ThriveX", "https://github.com/zxilong37", "2069065992@qq.com"))
                .build();
    }
}
