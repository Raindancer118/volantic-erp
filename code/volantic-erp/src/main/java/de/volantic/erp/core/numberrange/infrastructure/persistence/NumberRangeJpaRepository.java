package de.volantic.erp.core.numberrange.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface NumberRangeJpaRepository extends JpaRepository<NumberRangeEntity, UUID> {

    boolean existsByRangeKey(String rangeKey);

    Optional<NumberRangeEntity> findByRangeKey(String rangeKey);

    /** Pessimistic write lock (SELECT ... FOR UPDATE) so concurrent allocations are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from NumberRangeEntity r where r.rangeKey = :rangeKey")
    Optional<NumberRangeEntity> findForUpdateByRangeKey(@Param("rangeKey") String rangeKey);
}
