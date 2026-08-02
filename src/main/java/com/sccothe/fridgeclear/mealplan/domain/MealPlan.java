package com.sccothe.fridgeclear.mealplan.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_plan")
public class MealPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    private Long aiPlanRunId;
    @Column(nullable = false, length = 128) private String title;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private MealPlanEnums.PlanStatus status;
    @Lob @Column(columnDefinition = "longtext") private String constraintsJson;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { var now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public Long getAiPlanRunId() { return aiPlanRunId; }
    public void setAiPlanRunId(Long value) { aiPlanRunId = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate value) { startDate = value; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate value) { endDate = value; }
    public MealPlanEnums.PlanStatus getStatus() { return status; }
    public void setStatus(MealPlanEnums.PlanStatus value) { status = value; }
    public String getConstraintsJson() { return constraintsJson; }
    public void setConstraintsJson(String value) { constraintsJson = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
