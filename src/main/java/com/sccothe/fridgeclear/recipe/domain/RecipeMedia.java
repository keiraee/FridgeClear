package com.sccothe.fridgeclear.recipe.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "recipe_media")
public class RecipeMedia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long recipeId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RecipeEnums.MediaType mediaType;
    @Column(nullable = false, length = 512) private String sourcePath;
    @Column(length = 255) private String altText;
    @Column(nullable = false) private Integer sortOrder;
    public Long getId() { return id; }
    public void setRecipeId(Long v) { recipeId = v; }
    public void setMediaType(RecipeEnums.MediaType v) { mediaType = v; }
    public void setSourcePath(String v) { sourcePath = v; }
    public void setAltText(String v) { altText = v; }
    public void setSortOrder(Integer v) { sortOrder = v; }
}
