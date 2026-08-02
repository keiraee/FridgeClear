package com.sccothe.fridgeclear.mealplan.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_plan_run")
public class AiPlanRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false, length = 128) private String modelName;
    @Column(nullable = false, length = 32) private String promptVersion;
    @Lob @Column(nullable = false, columnDefinition = "longtext") private String requestJson;
    @Lob @Column(columnDefinition = "longtext") private String responseJson;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private MealPlanEnums.AiRunStatus status;
    @Lob @Column(columnDefinition = "longtext") private String errorMessage;
    @Column(nullable = false) private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @PrePersist void onCreate() { if (startedAt == null) startedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public String getModelName() { return modelName; }
    public void setModelName(String value) { modelName = value; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String value) { promptVersion = value; }
    public String getRequestJson() { return requestJson; }
    public void setRequestJson(String value) { requestJson = value; }
    public String getResponseJson() { return responseJson; }
    public void setResponseJson(String value) { responseJson = value; }
    public MealPlanEnums.AiRunStatus getStatus() { return status; }
    public void setStatus(MealPlanEnums.AiRunStatus value) { status = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { errorMessage = value; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime value) { startedAt = value; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime value) { finishedAt = value; }
}
