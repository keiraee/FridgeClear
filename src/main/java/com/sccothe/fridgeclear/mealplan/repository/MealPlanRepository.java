package com.sccothe.fridgeclear.mealplan.repository;

import com.sccothe.fridgeclear.mealplan.domain.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
}
