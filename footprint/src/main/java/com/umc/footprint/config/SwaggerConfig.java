package com.umc.footprint.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.RequestParameterBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ParameterType;
import springfox.documentation.service.RequestParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebMvc
@Profile({"local", "dev"})
public class SwaggerConfig {

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Footprint Spring Boot REST API")
                .version("1.2.0")
                .description("발자국 API Swagger API")
                .build();
    }

    @Bean
    public Docket api() {
        RequestParameterBuilder parameterBuilder = new RequestParameterBuilder();
        parameterBuilder.name("X-ACCESS-TOKEN")
                .description("사용자 인증을 위한 JWT 토큰")
                .in(ParameterType.HEADER)
                .required(false)
                .build();

        List<RequestParameter> parameters = new ArrayList<>();
        parameters.add(parameterBuilder.build());

        return new Docket(DocumentationType.OAS_30) // 3.0 문서버전으로 세팅
                .useDefaultResponseMessages(true)
                .globalRequestParameters(parameters)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.umc.footprint.src"))
                .paths(PathSelectors.any())
                .build();
    }
    
}
