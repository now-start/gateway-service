package org.nowstart.gateway.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    ADMIN, USER;

    public String authority() {
        return "ROLE_" + name();
    }
}