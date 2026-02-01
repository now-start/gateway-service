package org.nowstart.gateway.config;


import java.util.Comparator;
import java.util.List;
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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

@Configuration
@RefreshScope
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthorizeExchangeProperties authorizeProperties;
    private final CustomAuthoritiesFilter customAuthoritiesFilter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .addFilterAfter(customAuthoritiesFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .headers(headers -> headers
                        .frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable)
                        .contentSecurityPolicy(csp -> csp.policyDirectives("upgrade-insecure-requests; frame-ancestors 'self'"))
                )
                .authorizeExchange(this::configureAuthorization)
                .oauth2Login(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    private void configureAuthorization(ServerHttpSecurity.AuthorizeExchangeSpec exchanges) {
        List<AuthorizeExchangeProperties.PathRule> sortedRules = authorizeProperties.getRules().stream()
                .sorted((r1, r2) -> pathMatcher.getPatternComparator(r1.path()).compare(r1.path(), r2.path()))
                .toList();

        for (AuthorizeExchangeProperties.PathRule rule : sortedRules) {
            if (CollectionUtils.isEmpty(rule.roles())) {
                exchanges.pathMatchers(rule.path()).permitAll();
            } else {
                String[] authorities = rule.roles().stream()
                        .map(Role::name)
                        .toArray(String[]::new);
                exchanges.pathMatchers(rule.path()).hasAnyAuthority(authorities);
            }
        }

        exchanges.anyExchange().authenticated();
    }

}
