package com.sccothe.fridgeclear.pantry.service;

import com.sccothe.fridgeclear.common.api.ResourceNotFoundException;
import com.sccothe.fridgeclear.pantry.api.PantryItemDtos;
import com.sccothe.fridgeclear.pantry.domain.PantryItem;
import com.sccothe.fridgeclear.pantry.domain.PantryItemStatus;
import com.sccothe.fridgeclear.pantry.repository.PantryItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class PantryItemService {
    private static final long DEMO_USER_ID = 1L;

    private final PantryItemRepository repository;

    public PantryItemService(PantryItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<PantryItemDtos.Response> list(PantryItemStatus status, Pageable pageable) {
        Page<PantryItem> items = status == null
                ? repository.findByUserId(DEMO_USER_ID, pageable)
                : repository.findByUserIdAndStatus(DEMO_USER_ID, status, pageable);
        return items.map(item -> PantryItemDtos.Response.from(item, LocalDate.now()));
    }

    public PantryItemDtos.Response create(PantryItemDtos.CreateRequest request) {
        PantryItem item = new PantryItem();
        item.setUserId(DEMO_USER_ID);
        apply(item, request.rawName(), request.quantity(), request.unit(), request.purchaseDate(), request.expireDate(), request.note());
        return PantryItemDtos.Response.from(repository.save(item), LocalDate.now());
    }

    public PantryItemDtos.Response update(Long id, PantryItemDtos.UpdateRequest request) {
        PantryItem item = findOwned(id);
        apply(item, request.rawName(), request.quantity(), request.unit(), request.purchaseDate(), request.expireDate(), request.note());
        return PantryItemDtos.Response.from(item, LocalDate.now());
    }

    public PantryItemDtos.Response updateStatus(Long id, PantryItemDtos.StatusRequest request) {
        PantryItem item = findOwned(id);
        item.setStatus(request.status());
        return PantryItemDtos.Response.from(item, LocalDate.now());
    }

    public void delete(Long id) {
        PantryItem item = findOwned(id);
        repository.delete(item);
    }

    private PantryItem findOwned(Long id) {
        return repository.findById(id)
                .filter(item -> DEMO_USER_ID == item.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("库存食材不存在: " + id));
    }

    private void apply(
            PantryItem item,
            String rawName,
            java.math.BigDecimal quantity,
            String unit,
            java.time.LocalDate purchaseDate,
            java.time.LocalDate expireDate,
            String note
    ) {
        item.setRawName(rawName.trim());
        item.setQuantity(quantity);
        item.setUnit(unit);
        item.setPurchaseDate(purchaseDate);
        item.setExpireDate(expireDate);
        item.setNote(note);
    }
}
