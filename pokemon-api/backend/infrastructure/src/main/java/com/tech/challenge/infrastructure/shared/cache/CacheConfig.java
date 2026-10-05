package com.tech.challenge.infrastructure.shared.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache over Caffeine for the PokéAPI responses (D-15). The cache manager, its bounds and expiry come from
 * {@code spring.cache.*} in {@code application.yaml}.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
