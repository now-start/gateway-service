package org.nowstart.gateway.config;

import lombok.RequiredArgsConstructor;
import org.nowstart.gateway.data.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .addFilterAfter(customAuthoritiesFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/admin/applications/*/actuator/**").hasRole(Role.ADMINISTRATORS.name())
                .pathMatchers("/admin/instances/**").hasRole(Role.ADMINISTRATORS.name())
                .pathMatchers("/config/**").hasRole(Role.ADMINISTRATORS.name())
                .pathMatchers("/eureka/**").hasRole(Role.ADMINISTRATORS.name())
                .pathMatchers("/*/actuator/**").hasRole(Role.ADMINISTRATORS.name())

                .pathMatchers("/nyang-nyang-bot/authorization/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()

                .anyExchange().authenticated()
            )
            .oauth2Login(Customizer.withDefaults())
            .oauth2ResourceServer(oAuth2ResourceServerSpec ->
                oAuth2ResourceServerSpec.jwt(Customizer.withDefaults())
            )
            .build();
    }

    @Bean
    public WebFilter customAuthoritiesFilter() {
        return new CustomAuthoritiesFilter();
    }
}
