package org.nowstart.gateway.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.gateway.data.Role;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RefreshScope
public class CustomAuthoritiesFilter implements WebFilter {

    private static final String GROUPS_ATTRIBUTE = "groups";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> {
                if (ctx.getAuthentication() instanceof OAuth2AuthenticationToken auth) {
                    oAuth2Authentication(auth, ctx);
                } else if (ctx.getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
                    jwtAuthentication(jwtAuth, ctx);
                }
                    return ctx;
            })
                .flatMap(ctx ->
                        chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(ctx)))
                )
                .switchIfEmpty(chain.filter(exchange));
    }

    private void oAuth2Authentication(OAuth2AuthenticationToken auth, SecurityContext context) {
        OAuth2User principal = auth.getPrincipal();
        Object groupsObj = principal.getAttributes().get(GROUPS_ATTRIBUTE);
        Collection<String> groupNames = null;

        if (groupsObj instanceof Collection<?> groups) {
            groupNames = groups.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList();
        }

        List<GrantedAuthority> newAuthorities = mapGroupsToAuthorities(groupNames, auth.getAuthorities());
        context.setAuthentication(new OAuth2AuthenticationToken(principal, newAuthorities, auth.getAuthorizedClientRegistrationId()));
    }

    private void jwtAuthentication(JwtAuthenticationToken jwtAuth, SecurityContext context) {
        Jwt jwt = jwtAuth.getToken();
        List<String> groups = jwt.getClaimAsStringList(GROUPS_ATTRIBUTE);

        List<GrantedAuthority> newAuthorities = mapGroupsToAuthorities(groups, jwtAuth.getAuthorities());
        context.setAuthentication(new JwtAuthenticationToken(jwt, newAuthorities, jwt.getSubject()));
    }

    private List<GrantedAuthority> mapGroupsToAuthorities(Collection<String> groups, Collection<GrantedAuthority> existingAuthorities) {
        Map<String, GrantedAuthority> byName = new LinkedHashMap<>();
        for (GrantedAuthority authority : existingAuthorities) {
            byName.put(authority.getAuthority(), authority);
        }
        byName.putIfAbsent(Role.USERS.name(), new SimpleGrantedAuthority(Role.USERS.name()));

        if (groups != null) {
            groups.stream()
                    .map(group -> group.toUpperCase(Locale.ROOT))
                    .map(CustomAuthoritiesFilter::parseRole)
                    .filter(Objects::nonNull)
                    .map(role -> new SimpleGrantedAuthority(role.name()))
                    .forEach(authority -> byName.putIfAbsent(authority.getAuthority(), authority));
        }

        return List.copyOf(byName.values());
    }

    private static Role parseRole(String name) {
        try {
            return Role.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
