package org.nowstart.gateway.data;

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RoleTest {

    @Nested
    @DisplayName("GUEST 권한일 때")
    class WhenRoleIsGuest {

        @Test
        @DisplayName("자기 자신만 포함된다")
        void thenShouldContainOnlyItself() {
            // given
            var role = Role.GUEST;

            // when
            var included = role.getAllIncluded();

            // then
            then(included).containsExactly(Role.GUEST);
        }
    }

    @Nested
    @DisplayName("PERSONAL 권한일 때")
    class WhenRoleIsPersonal {

        @Test
        @DisplayName("자기 자신과 GUEST를 포함한다")
        void thenShouldContainItselfAndGuest() {
            // given
            var role = Role.PERSONAL;

            // when
            var included = role.getAllIncluded();

            // then
            then(included)
                    .containsExactlyInAnyOrder(Role.PERSONAL, Role.GUEST);
        }
    }

    @Nested
    @DisplayName("ADMINISTRATORS 권한일 때")
    class WhenRoleIsAdministrators {

        @Test
        @DisplayName("모든 권한을 포함한다")
        void thenShouldContainAllRoles() {
            // given
            var role = Role.ADMINISTRATORS;

            // when
            var included = role.getAllIncluded();

            // then
            then(included)
                    .containsExactlyInAnyOrder(Role.ADMINISTRATORS, Role.PERSONAL, Role.GUEST);
        }
    }

    @Nested
    @DisplayName("권한 하이어키 관계일 때")
    class WhenCheckingHierarchy {

        @Test
        @DisplayName("상위 권한은 하위 권한을 항상 포함한다")
        void thenHigherRoleShouldAlwaysIncludeLowerRoles() {
            // given
            var administrators = Role.ADMINISTRATORS;
            var personal = Role.PERSONAL;
            var guest = Role.GUEST;

            // when
            var administratorsIncluded = administrators.getAllIncluded();
            var personalIncluded = personal.getAllIncluded();
            var guestIncluded = guest.getAllIncluded();

            // then
            then(administratorsIncluded).containsAll(personalIncluded);
            then(personalIncluded).containsAll(guestIncluded);
        }

        @Test
        @DisplayName("하위 권한은 상위 권한을 포함하지 않는다")
        void thenLowerRoleShouldNotIncludeHigherRoles() {
            // given
            var guest = Role.GUEST;
            var personal = Role.PERSONAL;

            // when
            var guestIncluded = guest.getAllIncluded();
            var personalIncluded = personal.getAllIncluded();

            // then
            then(guestIncluded)
                    .doesNotContain(Role.PERSONAL, Role.ADMINISTRATORS);
            then(personalIncluded)
                    .doesNotContain(Role.ADMINISTRATORS);
        }

        @Test
        @DisplayName("중복 없이 반환된다")
        void thenShouldNotContainDuplicates() {
            // given & when & then
            for (Role role : Role.values()) {
                // when
                var included = role.getAllIncluded();
                var distinctCount = included.stream().distinct().count();

                // then
                then(distinctCount)
                        .as("Role %s에 중복이 있다", role)
                        .isEqualTo(included.size());
            }
        }
    }
}