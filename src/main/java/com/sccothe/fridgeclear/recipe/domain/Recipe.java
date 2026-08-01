package com.sccothe.fridgeclear.recipe.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipe", uniqueConstraints = {
        @UniqueConstraint(name = "uk_recipe_source_document", columnNames = "source_document_id"),
        @UniqueConstraint(name = "uk_recipe_slug", columnNames = "slug")
})
public class Recipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long sourceDocumentId;
    @Column(nullable = false, length = 128) private String name;
    @Column(nullable = false, length = 160) private String slug;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RecipeEnums.Category category;
    @Lob @Column(columnDefinition = "longtext") private String description;
    @Column(length = 32) private String difficultyText;
    private Byte difficultyLevel;
    @Column(precision = 10, scale = 2) private BigDecimal calories;
    @Column(length = 160) private String sourceTitle;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RecipeEnums.Status status;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { var now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getSourceDocumentId() { return sourceDocumentId; }
    public void setSourceDocumentId(Long v) { sourceDocumentId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getSlug() { return slug; }
    public void setSlug(String v) { slug = v; }
    public RecipeEnums.Category getCategory() { return category; }
    public void setCategory(RecipeEnums.Category v) { category = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public String getDifficultyText() { return difficultyText; }
    public void setDifficultyText(String v) { difficultyText = v; }
    public Byte getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(Byte v) { difficultyLevel = v; }
    public BigDecimal getCalories() { return calories; }
    public void setCalories(BigDecimal v) { calories = v; }
    public String getSourceTitle() { return sourceTitle; }
    public void setSourceTitle(String v) { sourceTitle = v; }
    public RecipeEnums.Status getStatus() { return status; }
    public void setStatus(RecipeEnums.Status v) { status = v; }
}
