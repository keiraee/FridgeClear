package com.sccothe.fridgeclear.mealplan.repository;

import com.sccothe.fridgeclear.mealplan.domain.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.sccothe.fridgeclear.mealplan.domain.MealPlanEnums;
import java.util.Optional;
import java.util.List;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    Page<MealPlan> findByUserId(Long userId, Pageable pageable);
    Page<MealPlan> findByUserIdAndStatus(Long userId, MealPlanEnums.PlanStatus status, Pageable pageable);
    Optional<MealPlan> findByIdAndUserId(Long id, Long userId);
    List<MealPlan> findByUserIdAndStatus(Long userId, MealPlanEnums.PlanStatus status);
}
