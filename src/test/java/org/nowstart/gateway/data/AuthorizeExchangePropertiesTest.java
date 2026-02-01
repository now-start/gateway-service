package org.nowstart.gateway.data;

import static org.assertj.core.api.BDDAssertions.then;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AuthorizeExchangePropertiesTest {

    private AuthorizeExchangeProperties givenProperties(
            List<String> administrators,
            List<String> personal,
            List<String> guest,
            List<String> publicPaths
    ) {
        AuthorizeExchangeProperties props = new AuthorizeExchangeProperties();
        props.setAdministrators(administrators);
        props.setPersonal(personal);
        props.setGuest(guest);
        props.setPublicPaths(publicPaths);
        return props;
    }

    @Nested
    @DisplayName("administrators 경로가 설정되어 있을 때")
    class WhenAdministratorsPathIsSet {

        @Test
        @DisplayName("해당 경로에 모든 권한이 부여된다")
        void thenAllRolesShouldBeGranted() {
            // given
            var props = givenProperties(List.of("/admin/**"), null, null, null);

            // when
            var rules = props.getRules();

            // then
            then(rules).hasSize(1);
            then(rules.get(0).path()).isEqualTo("/admin/**");
            then(rules.get(0).roles())
                    .containsExactlyInAnyOrder(Role.ADMINISTRATORS, Role.PERSONAL, Role.GUEST);
        }
    }

    @Nested
    @DisplayName("personal 경로가 설정되어 있을 때")
    class WhenPersonalPathIsSet {

        @Test
        @DisplayName("해당 경로에 PERSONAL과 GUEST 권한이 부여된다")
        void thenPersonalAndGuestRolesShouldBeGranted() {
            // given
            var props = givenProperties(null, List.of("/personal/**"), null, null);

            // when
            var rules = props.getRules();

            // then
            then(rules).hasSize(1);
            then(rules.get(0).path()).isEqualTo("/personal/**");
            then(rules.get(0).roles())
                    .containsExactlyInAnyOrder(Role.PERSONAL, Role.GUEST);
        }
    }

    @Nested
    @DisplayName("guest 경로가 설정되어 있을 때")
    class WhenGuestPathIsSet {

        @Test
        @DisplayName("해당 경로에 GUEST 권한만 부여된다")
        void thenOnlyGuestRoleShouldBeGranted() {
            // given
            var props = givenProperties(null, null, List.of("/guest/**"), null);

            // when
            var rules = props.getRules();

            // then
            then(rules).hasSize(1);
            then(rules.get(0).path()).isEqualTo("/guest/**");
            then(rules.get(0).roles())
                    .containsExactly(Role.GUEST);
        }
    }

    @Nested
    @DisplayName("publicPaths가 설정되어 있을 때")
    class WhenPublicPathsAreSet {

        @Test
        @DisplayName("해당 경로의 권한 리스트가 비어있다 (permitAll)")
        void thenRolesShouldBeEmpty() {
            // given
            var props = givenProperties(null, null, null, List.of("/actuator/**", "/public/**"));

            // when
            var rules = props.getRules();

            // then
            then(rules).hasSize(2);
            rules.forEach(rule -> then(rule.roles()).isEmpty());
        }
    }

    @Nested
    @DisplayName("모든 경로 타입이 혼합되어 있을 때")
    class WhenAllPathTypesAreCombined {

        @Test
        @DisplayName("각 경로마다 올바른 권한이 생성된다")
        void thenEachPathShouldHaveCorrectRoles() {
            // given
            var props = givenProperties(
                    List.of("/admin/**"),
                    List.of("/personal/**"),
                    List.of("/guest/**"),
                    List.of("/public/**")
            );

            // when
            var rules = props.getRules();
            var ruleMap = rules.stream()
                    .collect(Collectors.toMap(
                            AuthorizeExchangeProperties.PathRule::path,
                            AuthorizeExchangeProperties.PathRule::roles
                    ));

            // then
            then(rules).hasSize(4);
            then(ruleMap.get("/admin/**"))
                    .containsExactlyInAnyOrder(Role.ADMINISTRATORS, Role.PERSONAL, Role.GUEST);
            then(ruleMap.get("/personal/**"))
                    .containsExactlyInAnyOrder(Role.PERSONAL, Role.GUEST);
            then(ruleMap.get("/guest/**"))
                    .containsExactly(Role.GUEST);
            then(ruleMap.get("/public/**"))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("일부 속성이 null일 때")
    class WhenSomePropertiesAreNull {

        @Test
        @DisplayName("null 속성은 규칙에 포함되지 않는다")
        void thenNullPropertiesShouldBeExcluded() {
            // given
            var props = givenProperties(
                    List.of("/admin/**"),
                    null,   // personal은 null
                    null,   // guest도 null
                    List.of("/public/**")
            );

            // when
            var rules = props.getRules();

            // then
            then(rules).hasSize(2);
            then(rules.stream().map(AuthorizeExchangeProperties.PathRule::path))
                    .containsExactlyInAnyOrder("/admin/**", "/public/**");
        }
    }

    @Nested
    @DisplayName("속성이 빈 리스트일 때")
    class WhenPropertiesAreEmptyList {

        @Test
        @DisplayName("빈 리스트는 규칙을 생성하지 않는다")
        void thenNoRulesShouldBeCreated() {
            // given
            var props = givenProperties(
                    List.of(),                  // 빈 리스트
                    List.of("/personal/**"),
                    List.of(),                  // 빈 리스트
                    List.of()                   // 빈 리스트
            );

            // when
            var rules = props.getRules();

            // then
            then(rules).hasSize(1);
            then(rules.getFirst().path()).isEqualTo("/personal/**");
        }
    }

    @Nested
    @DisplayName("제시된 특정 보안 설정일 때")
    class WhenSpecificSecurityConfigIsProvided {

        @Test
        @DisplayName("모든 설정된 경로와 권한이 올바르게 매핑된다")
        void thenAllConfiguredPathsShouldBeMappedCorrectly() {
            // given
            var administrators = List.of(
                    "/admin/applications/*/actuator/**",
                    "/admin/instances/**",
                    "/config/**",
                    "/eureka/**",
                    "/*/actuator/**"
            );
            var publicPaths = List.of(
                    "/nyang-nyang-bot/authorization/**",
                    "/actuator/**",
                    "/config/actuator/busrefresh"
            );
            var props = givenProperties(administrators, List.of(), List.of(), publicPaths);

            // when
            var rules = props.getRules();
            var ruleMap = rules.stream()
                    .collect(Collectors.toMap(
                            AuthorizeExchangeProperties.PathRule::path,
                            AuthorizeExchangeProperties.PathRule::roles,
                            (existing, replacement) -> existing
                    ));

            // then
            then(rules).hasSize(administrators.size() + publicPaths.size());

            // Administrators 경로 검증
            administrators.forEach(path -> {
                then(ruleMap.get(path))
                        .as("Path %s should have administrator roles", path)
                        .containsExactlyInAnyOrder(Role.ADMINISTRATORS, Role.PERSONAL, Role.GUEST);
            });

            // Public 경로 검증
            publicPaths.forEach(path -> {
                then(ruleMap.get(path))
                        .as("Path %s should be public (no roles)", path)
                        .isEmpty();
            });
        }

        @Test
        @DisplayName("publicPaths에 설정된 경로는 빈 권한 리스트를 가져야 하며, 이는 SecurityConfig에서 permitAll()로 처리된다")
        void thenPublicPathsShouldHaveEmptyRolesForPermitAll() {
            // given
            var publicPaths = List.of("/health", "/info");
            var props = givenProperties(null, null, null, publicPaths);

            // when
            var rules = props.getRules();

            // then
            then(rules).hasSize(2);
            for (var rule : rules) {
                then(rule.roles())
                        .as("Public path %s must have empty roles for permitAll()", rule.path())
                        .isEmpty();
            }
        }
    }
}