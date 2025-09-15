package org.nowstart.gateway.data;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "security")
public class AuthorizeExchangeProperties {

    private List<String> administrators;
    private List<String> personal;
    private List<String> guest;
    private List<String> publicPaths;

    public List<PathRule> getRules() {
        List<PathRule> rules = new ArrayList<>();

        // 권한 상속: administrators는 모든 권한 포함, personal은 guest 포함
        if (administrators != null) {
            administrators.forEach(path -> rules.add(new PathRule(path, List.of(Role.ADMINISTRATORS))));
        }

        if (personal != null) {
            personal.forEach(path -> rules.add(new PathRule(path, List.of(Role.PERSONAL, Role.ADMINISTRATORS))));
        }

        if (guest != null) {
            guest.forEach(path -> rules.add(new PathRule(path, List.of(Role.GUEST, Role.PERSONAL, Role.ADMINISTRATORS))));
        }

        if (publicPaths != null) {
            publicPaths.forEach(path -> rules.add(new PathRule(path, List.of())));
        }

        return rules;
    }

    @Data
    @AllArgsConstructor
    public static class PathRule {
        private String path;
        private List<Role> roles;
    }
}