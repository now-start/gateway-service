package org.nowstart.gateway.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.nowstart.gateway.data.Role;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
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
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> {
                    Authentication authentication = ctx.getAuthentication();
                    if (authentication == null) {
                        return ctx;
                    }

                    ServerHttpRequest request = exchange.getRequest();
                    log.info("[{}] SecurityContext found method={} path={} authType={} principal={} authorities={}",
                            request.getId(),
                            request.getMethod(),
                            request.getPath(),
                            authentication.getClass().getSimpleName(),
                            authentication.getName(),
                            authentication.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .toList());

                    if (authentication instanceof OAuth2AuthenticationToken auth) {
                        oAuth2Authentication(auth, ctx);
                    } else if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                        jwtAuthentication(jwtAuth, ctx);
                    }
                    return ctx;
                })
                .map(ctx -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(ctx))))
                .switchIfEmpty(Mono.fromSupplier(() -> chain.filter(exchange)))
                .flatMap(Function.identity());
    }

    private void oAuth2Authentication(OAuth2AuthenticationToken auth, SecurityContext context) {
        OAuth2User principal = auth.getPrincipal();
        Object groupsObj = Objects.requireNonNull(principal).getAttributes().get(GROUPS_ATTRIBUTE);
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
