package org.nowstart.gateway.data;

import java.util.ArrayList;
import java.util.List;
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
            administrators.forEach(path ->
                    rules.add(new PathRule(path, Role.ADMINISTRATORS.getAllIncluded()))
            );
        }

        if (personal != null) {
            personal.forEach(path ->
                    rules.add(new PathRule(path, Role.PERSONAL.getAllIncluded()))
            );
        }

        if (guest != null) {
            guest.forEach(path ->
                    rules.add(new PathRule(path, Role.GUEST.getAllIncluded()))
            );
        }

        if (publicPaths != null) {
            publicPaths.forEach(path -> rules.add(new PathRule(path, List.of())));
        }

        return rules;
    }

    public record PathRule(String path, List<Role> roles) {
    }
}