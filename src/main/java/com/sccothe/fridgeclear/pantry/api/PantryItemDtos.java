package com.sccothe.fridgeclear.pantry.api;

import com.sccothe.fridgeclear.pantry.domain.PantryItem;
import com.sccothe.fridgeclear.pantry.domain.PantryItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PantryItemDtos {
    private PantryItemDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 128) String rawName,
            @DecimalMin(value = "0.001", inclusive = true) BigDecimal quantity,
            @Size(max = 32) String unit,
            LocalDate purchaseDate,
            LocalDate expireDate,
            @Size(max = 255) String note
    ) {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 128) String rawName,
            @DecimalMin(value = "0.001", inclusive = true) BigDecimal quantity,
            @Size(max = 32) String unit,
            LocalDate purchaseDate,
            LocalDate expireDate,
            @Size(max = 255) String note
    ) {
    }

    public record StatusRequest(@NotNull PantryItemStatus status) {
    }

    public record Response(
            Long id,
            String rawName,
            Long ingredientId,
            String ingredientName,
            BigDecimal quantity,
            String unit,
            LocalDate purchaseDate,
            LocalDate expireDate,
            PantryItemStatus status,
            boolean expiringSoon,
            String note
    ) {
        public static Response from(PantryItem item, LocalDate today, String ingredientName) {
            boolean expiringSoon = item.getExpireDate() != null
                    && !item.getExpireDate().isBefore(today)
                    && !item.getExpireDate().isAfter(today.plusDays(3));
            return new Response(
                    item.getId(),
                    item.getRawName(),
                    item.getIngredientId(),
                    ingredientName,
                    item.getQuantity(),
                    item.getUnit(),
                    item.getPurchaseDate(),
                    item.getExpireDate(),
                    item.getStatus(),
                    expiringSoon,
                    item.getNote()
            );
        }
    }
}
