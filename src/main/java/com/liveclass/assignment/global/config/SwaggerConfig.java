package com.liveclass.assignment.global.config;


import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(apiInfo())
        .servers(apiServers())
        .externalDocs(externalDocumentation());
  }

  private Info apiInfo() {
    return new Info()
        .title("Liveclass 수강 신청 시스템 API")
        .version("v1.0.0")
        .description("""
            Liveclass 사전 과제 - BE-A. 수강 신청 시스템 API 문서입니다.
            
            주요 기능:
            - 강의 생성 및 조회
            - 강의 상태 변경
            - 수강 신청
            - 결제 확정
            - 수강 취소
            - 정원 초과 방지 및 동시성 제어
            """)
        .contact(new Contact()
            .name("devJin11")
            .url("https://github.com/devJin11/liveclass-assignment"));
  }

  private List<Server> apiServers() {
    return List.of(
        new Server()
            .url("http://localhost:8080")
            .description("Local / Docker Compose Server")
    );
  }

  private ExternalDocumentation externalDocumentation() {
    return new ExternalDocumentation()
        .description("GitHub Repository")
        .url("https://github.com/devJin11/liveclass-assignment");
  }
}
