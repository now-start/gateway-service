package org.nowstart.gateway.data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public enum Role {
    USERS,
    ADMINISTRATORS;

    private final List<Role> includes;

    Role(Role... includes) {
        this.includes = Arrays.asList(includes);
    }

    private static Stream<Role> apply(Role r) {
        return r.getAllIncluded().stream();
    }

    public List<Role> getAllIncluded() {
        return Stream.concat(
                Stream.of(this),
                includes.stream().flatMap(Role::apply)
        ).distinct().toList();
    }
}
