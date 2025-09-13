package org.nowstart.gateway.config;


import lombok.RequiredArgsConstructor;
import org.nowstart.gateway.data.AuthorizeExchangeProperties;
import org.nowstart.gateway.data.Role;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.CollectionUtils;

@RefreshScope
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthorizeExchangeProperties authorizeProperties;
    private final CustomAuthoritiesFilter customAuthoritiesFilter;

    @Bean
    @RefreshScope
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .addFilterAfter(customAuthoritiesFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange(this::configureAuthorization)
            .oauth2Login(Customizer.withDefaults())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }

    private void configureAuthorization(ServerHttpSecurity.AuthorizeExchangeSpec exchanges) {
        for (AuthorizeExchangeProperties.PathRule rule : authorizeProperties.getRules()) {
            if (CollectionUtils.isEmpty(rule.getRoles())) {
                exchanges.pathMatchers(rule.getPath()).permitAll();
            } else {
                String[] authorities = rule.getRoles().stream()
                    .map(Role::name)
                    .toArray(String[]::new);
                exchanges.pathMatchers(rule.getPath()).hasAnyAuthority(authorities);
            }
        }

        exchanges.anyExchange().authenticated();
    }

}
