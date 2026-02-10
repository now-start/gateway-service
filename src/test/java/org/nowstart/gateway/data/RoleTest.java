package org.nowstart.gateway.data;

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RoleTest {

    @Nested
    @DisplayName("USERS role")
    class WhenRoleIsUsers {

        @Test
        @DisplayName("contains only itself")
        void thenShouldContainOnlyItself() {
            // given
            var role = Role.USERS;

            // when
            var included = role.getAllIncluded();

            // then
            then(included).containsExactly(Role.USERS);
        }
    }

    @Nested
    @DisplayName("ADMINISTRATORS role")
    class WhenRoleIsAdministrators {

        @Test
        @DisplayName("contains only itself")
        void thenShouldContainOnlyItself() {
            // given
            var role = Role.ADMINISTRATORS;

            // when
            var included = role.getAllIncluded();

            // then
            then(included)
                    .containsExactly(Role.ADMINISTRATORS);
        }
    }

    @Nested
    @DisplayName("duplicate check")
    class WhenCheckingDuplicates {

        @Test
        @DisplayName("returns distinct roles")
        void thenShouldNotContainDuplicates() {
            // given & when & then
            for (Role role : Role.values()) {
                // when
                var included = role.getAllIncluded();
                var distinctCount = included.stream().distinct().count();

                // then
                then(distinctCount)
                        .as("Role %s has duplicates", role)
                        .isEqualTo(included.size());
            }
        }
    }
}
