package com.tech.challenge.web.shared.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.tech.challenge.infrastructure.user.security.token.JwtConfig;

/**
 * Public and protected routes (D-07, D-11): everything that is not public needs a valid JWT.
 * Imports {@link JwtConfig} so every test that imports this class also gets the {@code JwtDecoder}.
 */
@Configuration
@Import(JwtConfig.class)
public class SecurityConfig {

    private static final RequestMatcher PUBLIC_ROUTES = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/**"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/v1/pokemon/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/error"));

    @Bean
    SecurityFilterChain securityFilterChain(final HttpSecurity http,
            @Qualifier("handlerExceptionResolver") final HandlerExceptionResolver handlerExceptionResolver)
            throws Exception {
        final var bearerTokenResolver = new DefaultBearerTokenResolver();
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        // A stale token must not turn a public request into a 401.
                        .bearerTokenResolver(request -> PUBLIC_ROUTES.matches(request) ? null : bearerTokenResolver.resolve(request))
                        // GlobalExceptionHandler builds the 401 ProblemDetail, like every other error (D-17).
                        .authenticationEntryPoint((request, response, e) ->
                                handlerExceptionResolver.resolveException(request, response, null, e)))
                .build();
    }
}
