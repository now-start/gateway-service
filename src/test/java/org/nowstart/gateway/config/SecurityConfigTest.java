package org.nowstart.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nowstart.gateway.data.AuthorizeExchangeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@ActiveProfiles("test")
@SpringBootTest(classes = {SecurityConfig.class, AuthorizeExchangeProperties.class, CustomAuthoritiesFilter.class})
@ImportAutoConfiguration({
        ReactiveWebSecurityAutoConfiguration.class,
        WebFluxAutoConfiguration.class,
        RefreshAutoConfiguration.class
})
class SecurityConfigTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;
    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(SecurityMockServerConfigurers.springSecurity())
                .configureClient()
                .build();
    }

    @Test
    @DisplayName("더 구체적인 public 경로(/config/actuator/busrefresh)가 generic한 admin 경로(/config/**)보다 우선순위가 높아야 한다")
    void specificPublicPathShouldBePrioritizedOverGenericAdminPath() {
        // given
        String publicPath = "/config/actuator/busrefresh";

        // when & then
        webTestClient.get()
                .uri(publicPath)
                .exchange()
                .expectStatus().isNotFound(); // permitAll() 이므로 404
    }

    @Test
    @DisplayName("public 경로가 아닌 다른 /config/** 경로는 인증이 필요하다")
    void genericAdminPathShouldStillRequireAuthentication() {
        // given
        String adminPath = "/config/other";

        // when & then
        webTestClient.get()
                .uri(adminPath)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public HttpExchangeRepository httpExchangeRepository() {
            return new InMemoryHttpExchangeRepository();
        }

        @Bean
        public AuditEventRepository auditEventRepository() {
            return new InMemoryAuditEventRepository();
        }
    }
}
