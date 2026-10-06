package com.challenge.aitools.taskmanagement.web.shared.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.challenge.aitools.taskmanagement.infrastructure.user.security.token.JwtConfig;

@Configuration
@Import(JwtConfig.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(final HttpSecurity http,
            @Qualifier("handlerExceptionResolver") final HandlerExceptionResolver handlerExceptionResolver)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/register"))
                        .permitAll()
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/login"))
                        .permitAll()
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/error")).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, e) ->
                                handlerExceptionResolver.resolveException(request, response, null, e)))
                .build();
    }
}
