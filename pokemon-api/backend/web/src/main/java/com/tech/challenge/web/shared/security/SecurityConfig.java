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
                        .bearerTokenResolver(request -> PUBLIC_ROUTES.matches(request) ? null : bearerTokenResolver.resolve(request))
                        .authenticationEntryPoint((request, response, e) ->
                                handlerExceptionResolver.resolveException(request, response, null, e)))
                .build();
    }
}
