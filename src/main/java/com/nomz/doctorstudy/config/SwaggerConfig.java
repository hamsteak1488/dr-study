package com.nomz.doctorstudy.config;

import com.nomz.doctorstudy.member.LoginToken;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.ServletContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI(ServletContext servletContext) {
        Info info = new Info()
                .version("v1.0")
                .title("Doctor Study API")
                .description("닥터 스터디 프로젝트 API");

        SecurityScheme devMemberIdScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(LoginToken.DEV_LOGIN_TOKEN)
                .scheme("bearer"); // API Key 스키마로 설정

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(LoginToken.DEV_LOGIN_TOKEN);

        Server server = new Server()
                .url(servletContext.getContextPath());

        return new OpenAPI()
                .info(info)
                .components(new Components().addSecuritySchemes(LoginToken.DEV_LOGIN_TOKEN, devMemberIdScheme))
                .security(List.of(securityRequirement))
                .servers(List.of(server));
    }
}
