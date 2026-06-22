package de.volantic.erp.security.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.security.infrastructure.persistence.CacheNames;
import de.volantic.erp.security.infrastructure.persistence.CachedOrgTree;
import de.volantic.erp.security.infrastructure.persistence.CachedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Redis caching for the security module. The authorization snapshot ({@link CachedUser}) is stored as
 * typed JSON with a short TTL.
 *
 * <p>Resilience: cache errors do <strong>not</strong> fail requests. The {@link CacheErrorHandler}
 * logs and swallows them so a Redis outage degrades gracefully to the database (fail-open) — important
 * because Redis is part of the bundle and may restart (ADR-0005).
 */
@Configuration
@EnableCaching
class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);
    private static final Duration USER_PERMISSIONS_TTL = Duration.ofMinutes(5);
    private static final Duration ORG_TREE_TTL = Duration.ofMinutes(10);

    @Bean
    RedisCacheManagerBuilderCustomizer securityCacheCustomizer(ObjectMapper objectMapper) {
        var userSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, CachedUser.class);
        var treeSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, CachedOrgTree.class);
        return builder -> builder
                .withCacheConfiguration(
                        CacheNames.USER_PERMISSIONS,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(USER_PERMISSIONS_TTL)
                                // Null values ARE cached (negative caching) to shield the DB from repeated
                                // lookups of valid-but-unmirrored subjects; staleness is bounded by the TTL
                                // and cleared by write-side eviction.
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(userSerializer)))
                .withCacheConfiguration(
                        CacheNames.ORG_TREE,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(ORG_TREE_TTL)
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(treeSerializer)));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("cache get failed [{}::{}] — serving from source: {}", cache.getName(), key, exception.toString());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("cache put failed [{}::{}]: {}", cache.getName(), key, exception.toString());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("cache evict failed [{}::{}]: {}", cache.getName(), key, exception.toString());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("cache clear failed [{}]: {}", cache.getName(), exception.toString());
            }
        };
    }
}
