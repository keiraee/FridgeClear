package com.sccothe.fridgeclear.pantry.repository;

import com.sccothe.fridgeclear.pantry.domain.PantryItem;
import com.sccothe.fridgeclear.pantry.domain.PantryItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {
    Page<PantryItem> findByUserIdAndStatus(Long userId, PantryItemStatus status, Pageable pageable);

    Page<PantryItem> findByUserId(Long userId, Pageable pageable);

    List<PantryItem> findByUserIdAndStatusOrderByExpireDateAscIdAsc(Long userId, PantryItemStatus status);
}
