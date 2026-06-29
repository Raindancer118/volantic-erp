package de.volantic.erp.security.infrastructure.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * Provides an in-memory {@link CacheManager} for persistence slice tests, replacing Redis (absent in
 * {@code @DataJpaTest}) so the {@code @Cacheable} on {@link OrgTreeCache#load()} stays active.
 *
 * <p>Deliberately a <em>top-level</em> imported {@link TestConfiguration} rather than a class nested in the
 * test: a nested {@code @Configuration} inside a {@code @DataJpaTest} is auto-detected as the bootstrap
 * configuration, which suppresses discovery of the application's {@code @SpringBootConfiguration} and breaks
 * {@code @EnableAutoConfiguration} base-package resolution.
 */
@TestConfiguration(proxyBeanMethods = false)
@EnableCaching
class OrgTreeTestCacheConfig {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(CacheNames.ORG_TREE);
    }
}
