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

        List<GrantedAuthority> newAuthorities = mapGroupsToAuthorities(groupNames, auth);

        context.setAuthentication(new OAuth2AuthenticationToken(principal, newAuthorities, auth.getAuthorizedClientRegistrationId()));
    }

    private List<GrantedAuthority> mapGroupsToAuthorities(List<String> groups, OAuth2AuthenticationToken auth) {
        return groups.stream()
            .map(String::toUpperCase)
            .map(CustomAuthoritiesFilter::apply)
            .filter(Objects::nonNull)
            .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.name()))
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new java.util.ArrayList<>(auth.getAuthorities())),
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
