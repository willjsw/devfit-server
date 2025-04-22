package com.amcamp.global.config.security;

import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.security.config.Customizer.withDefaults;

import com.amcamp.domain.auth.application.JwtTokenService;
import com.amcamp.global.common.constants.UrlConstants;
import com.amcamp.global.helper.SpringEnvironmentHelper;
import com.amcamp.global.security.JwtAuthenticationFilter;
import com.amcamp.global.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final SpringEnvironmentHelper springEnvironmentHelper;
    private final JwtTokenService jwtTokenService;
    private final CookieUtil cookieUtil;

    @Value("${swagger.username}")
    private String swaggerUsername;

    @Value("${swagger.password}")
    private String swaggerPassword;

    private void defaultFilterChain(HttpSecurity http) throws Exception {
        http.httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        UserDetails user =
                User.withUsername(swaggerUsername)
                        .password(passwordEncoder().encode(swaggerPassword))
                        .roles("SWAGGER")
                        .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    @Profile({"prod", "dev", "local"})
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
        defaultFilterChain(http);

        http.securityMatcher("/swagger-ui/**", "/v3/api-docs/**").httpBasic(withDefaults());

        http.authorizeHttpRequests(
                springEnvironmentHelper.isProdProfile() || springEnvironmentHelper.isDevProfile()
                        ? auth -> auth.anyRequest().authenticated()
                        : auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        defaultFilterChain(http);

        http.authorizeHttpRequests(
                auth ->
                        auth.requestMatchers("/devfit-actuator/**")
                                .permitAll()
                                .requestMatchers("/feedbacks/**")
                                .authenticated()
                                .requestMatchers("/**")
                                .permitAll()
                                .requestMatchers("/feedbacks/**")
                                .authenticated()
                                .anyRequest()
                                .authenticated());

        http.addFilterBefore(
                jwtAuthenticationFilter(jwtTokenService, cookieUtil),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (springEnvironmentHelper.isProdProfile()) {
            configuration.addAllowedOriginPattern(UrlConstants.PROD_DOMAIN_URL);
        }

        if (springEnvironmentHelper.isDevProfile()) {
            configuration.addAllowedOriginPattern(UrlConstants.DEV_DOMAIN_URL);
            configuration.addAllowedOriginPattern(UrlConstants.LOCAL_DOMAIN_URL);
        }

        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.addExposedHeader(SET_COOKIE);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenService jwtTokenService, CookieUtil cookieUtil) {
        return new JwtAuthenticationFilter(jwtTokenService, cookieUtil);
    }
}
