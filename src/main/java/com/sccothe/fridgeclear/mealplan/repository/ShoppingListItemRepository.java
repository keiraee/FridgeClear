package com.sccothe.fridgeclear.mealplan.repository;

import com.sccothe.fridgeclear.mealplan.domain.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {
    List<ShoppingListItem> findByMealPlanIdOrderByIdAsc(Long mealPlanId);
}
