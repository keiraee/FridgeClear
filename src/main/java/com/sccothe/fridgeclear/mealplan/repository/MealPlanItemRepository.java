package com.sccothe.fridgeclear.mealplan.repository;

import com.sccothe.fridgeclear.mealplan.domain.MealPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MealPlanItemRepository extends JpaRepository<MealPlanItem, Long> {
    List<MealPlanItem> findByMealPlanIdOrderByPlanDateAscSortOrderAsc(Long mealPlanId);
    Optional<MealPlanItem> findByIdAndMealPlanId(Long id, Long mealPlanId);
}
