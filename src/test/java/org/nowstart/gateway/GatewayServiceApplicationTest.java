package org.nowstart.gateway;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class GatewayServiceApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private HttpExchangeRepository httpExchangeRepository;

    @Autowired(required = false)
    private AuditEventRepository auditEventRepository;

    @Test
    @DisplayName("애플리케이션 컨텍스트가 정상적으로 로드되어야 한다")
    void contextLoads() {
        then(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("필수 빈들이 정상적으로 생성되어야 한다")
    void beansAreCreated() {
        then(httpExchangeRepository).isNotNull();
        then(auditEventRepository).isNotNull();
    }

    @Test
    @DisplayName("main 메서드가 예외 없이 실행되어야 한다")
    void mainMethodExecutesWithoutException() {
        // main 메서드 실행 시 Spring Boot 애플리케이션이 구동됩니다.
        // 테스트 환경에서는 이미 컨텍스트가 로드되어 있을 수 있지만, 
        // 커버리지를 위해 명시적으로 호출합니다.
        // 실제 실행을 방지하기 위해 Mocking을 할 수도 있으나, 
        // 일반적으로 main 메서드 커버리지는 단순 호출로 처리합니다.
        GatewayServiceApplication.main(new String[]{"--spring.profiles.active=test"});
    }
}
