package org.nowstart.gateway.config;

import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.nowstart.gateway.data.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${encrypt.key}")
    private String secretKey;
    private static final int SECRET_KEY_MIN_LENGTH = 32;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/*/actuator/**").hasRole(Role.ADMIN.name())

                        .pathMatchers("/admin/applications/*/actuator/**").hasRole(Role.ADMIN.name())
                        .pathMatchers("/admin/instances/**").hasRole(Role.ADMIN.name())
                        .pathMatchers("/admin/**").permitAll()

                        .pathMatchers("/config/**").hasRole(Role.ADMIN.name())
                        .pathMatchers("/eureka/**").hasRole(Role.ADMIN.name())

                        .pathMatchers("/nyang-nyang-bot/authorization/**").permitAll()

                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                ).build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withSecretKey(new SecretKeySpec(StringUtils.leftPad(secretKey, SECRET_KEY_MIN_LENGTH, '0').getBytes(), "HmacSHA256")).build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        return new ReactiveJwtAuthenticationConverterAdapter(jwt -> new JwtAuthenticationToken(jwt,
                jwt.getClaimAsStringList("roles").stream()
                        .map(role -> new SimpleGrantedAuthority(Role.valueOf(role).authority()))
                        .toList()));
    }
}
