package com.sccothe.fridgeclear.mealplan.repository;

import com.sccothe.fridgeclear.mealplan.domain.AiPlanRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiPlanRunRepository extends JpaRepository<AiPlanRun, Long> {
    Optional<AiPlanRun> findByIdAndUserId(Long id, Long userId);
}
