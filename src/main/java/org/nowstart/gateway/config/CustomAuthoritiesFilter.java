package org.nowstart.gateway.config;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.gateway.data.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
public class CustomAuthoritiesFilter implements WebFilter {

    private static final String GROUPS_ATTRIBUTE = "groups";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .doOnNext(ctx -> {
                if (ctx.getAuthentication() instanceof OAuth2AuthenticationToken auth) {
                    oAuth2Authentication(auth, ctx);
                } else if (ctx.getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
                    jwtAuthentication(jwtAuth, ctx);
                }
            })
            .then(chain.filter(exchange));
    }

    private void oAuth2Authentication(OAuth2AuthenticationToken auth, SecurityContext context) {
        OAuth2User principal = auth.getPrincipal();
        Collection<?> groups = (Collection<?>) principal.getAttributes().get(GROUPS_ATTRIBUTE);

        List<String> groupNames = groups.stream()
            .filter(Objects::nonNull)
            .map(Object::toString)
            .toList();

        List<GrantedAuthority> newAuthorities = mapGroupsToAuthorities(groupNames, auth.getAuthorities());

        context.setAuthentication(new OAuth2AuthenticationToken(principal, newAuthorities, auth.getAuthorizedClientRegistrationId()));
    }

    private void jwtAuthentication(JwtAuthenticationToken jwtAuth, SecurityContext context) {
        Jwt jwt = jwtAuth.getToken();
        Collection<String> groups = jwt.getClaimAsStringList(GROUPS_ATTRIBUTE);

        List<GrantedAuthority> newAuthorities = mapGroupsToAuthorities(groups, jwtAuth.getAuthorities());

        context.setAuthentication(new JwtAuthenticationToken(jwt, newAuthorities, jwt.getSubject()));
    }

    private List<GrantedAuthority> mapGroupsToAuthorities(Collection<String> groups, Collection<GrantedAuthority> existingAuthorities) {
        return groups.stream()
            .map(String::toUpperCase)
            .map(CustomAuthoritiesFilter::apply)
            .filter(Objects::nonNull)
            .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.name()))
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new java.util.ArrayList<>(existingAuthorities)),
                List::copyOf
            ));
    }

    private static Role apply(String name) {
        try {
            return Role.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
