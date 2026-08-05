package com.sccothe.fridgeclear.ai.repository;

import com.sccothe.fridgeclear.ai.domain.SystemAiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SystemAiConfigRepository extends JpaRepository<SystemAiConfig, Long> {
    default Optional<SystemAiConfig> findSingleton() {
        return findTopByOrderByIdAsc();
    }

    Optional<SystemAiConfig> findTopByOrderByIdAsc();

    @Modifying
    @Query("DELETE FROM SystemAiConfig c WHERE c.id <> :keepId")
    void deleteAllExcept(@Param("keepId") Long keepId);
}
