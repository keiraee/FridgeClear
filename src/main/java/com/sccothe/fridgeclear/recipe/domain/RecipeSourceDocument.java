package com.sccothe.fridgeclear.recipe.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipe_source_document")
public class RecipeSourceDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 255) private String sourceRepository;
    @Column(nullable = false, length = 64) private String sourceCommit;
    @Column(nullable = false, length = 512) private String sourcePath;
    @Column(nullable = false, length = 64) private String sourceIdentityHash;
    @Column(nullable = false, length = 64) private String fileHash;
    @Lob @Column(nullable = false, columnDefinition = "longtext") private String rawMarkdown;
    @Column(nullable = false, length = 32) private String parserVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RecipeEnums.ImportStatus importStatus;
    @Lob @Column(columnDefinition = "longtext") private String importError;
    @Column(nullable = false) private LocalDateTime importedAt;

    public Long getId() { return id; }
    public String getSourceRepository() { return sourceRepository; }
    public void setSourceRepository(String v) { sourceRepository = v; }
    public String getSourceCommit() { return sourceCommit; }
    public void setSourceCommit(String v) { sourceCommit = v; }
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String v) { sourcePath = v; }
    public String getSourceIdentityHash() { return sourceIdentityHash; }
    public void setSourceIdentityHash(String v) { sourceIdentityHash = v; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String v) { fileHash = v; }
    public String getRawMarkdown() { return rawMarkdown; }
    public void setRawMarkdown(String v) { rawMarkdown = v; }
    public String getParserVersion() { return parserVersion; }
    public void setParserVersion(String v) { parserVersion = v; }
    public RecipeEnums.ImportStatus getImportStatus() { return importStatus; }
    public void setImportStatus(RecipeEnums.ImportStatus v) { importStatus = v; }
    public String getImportError() { return importError; }
    public void setImportError(String v) { importError = v; }
    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime v) { importedAt = v; }
}
