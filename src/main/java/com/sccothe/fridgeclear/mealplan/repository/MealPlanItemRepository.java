package com.sccothe.fridgeclear.mealplan.repository;

import com.sccothe.fridgeclear.mealplan.domain.MealPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MealPlanItemRepository extends JpaRepository<MealPlanItem, Long> {
    List<MealPlanItem> findByMealPlanIdOrderByPlanDateAscSortOrderAsc(Long mealPlanId);
}
