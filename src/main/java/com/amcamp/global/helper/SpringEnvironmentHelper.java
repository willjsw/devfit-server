package com.amcamp.global.helper;

import java.util.Arrays;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringEnvironmentHelper {

    private final Environment environment;

    public static final String PROD = "prod";
    public static final String DEV = "dev";
    public static final String LOCAL = "local";

    public String getCurrentProfile() {
        return getActiveProfiles()
                .filter(profile -> profile.equals(PROD) || profile.equals(DEV))
                .findFirst()
                .orElse(LOCAL);
    }

    public Boolean isProdProfile() {
        return getActiveProfiles().anyMatch(PROD::equals);
    }

    public Boolean isDevProfile() {
        return getActiveProfiles().anyMatch(DEV::equals);
    }

    private Stream<String> getActiveProfiles() {
        return Arrays.stream(environment.getActiveProfiles());
    }
}
