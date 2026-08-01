package com.sccothe.fridgeclear.recipe.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "ingredient_alias", uniqueConstraints = @UniqueConstraint(name = "uk_ingredient_alias_normalized", columnNames = "normalized_alias"))
public class IngredientAlias {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long ingredientId;
    @Column(nullable = false, length = 128) private String aliasName;
    @Column(nullable = false, length = 128) private String normalizedAlias;

    public Long getId() { return id; }
    public Long getIngredientId() { return ingredientId; }
    public String getAliasName() { return aliasName; }
    public String getNormalizedAlias() { return normalizedAlias; }
    public void setIngredientId(Long v) { ingredientId = v; }
    public void setAliasName(String v) { aliasName = v; }
    public void setNormalizedAlias(String v) { normalizedAlias = v; }
}
