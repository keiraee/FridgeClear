package com.sccothe.fridgeclear.recipe.importer;

import com.sccothe.fridgeclear.recipe.domain.*;
import com.sccothe.fridgeclear.recipe.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HowToCookImporter {
    private final RecipeSourceDocumentRepository sourceRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository ingredientAliasRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeMediaRepository recipeMediaRepository;
    private final HowToCookParser parser = new HowToCookParser();
    private static final Pattern QUANTITY = Pattern.compile("(\\d+(?:\\.\\d+)?)(?:\\s*-\\s*(\\d+(?:\\.\\d+)?))?\\s*(克|g|千克|kg|毫升|ml|升|l|个|只|斤|片|瓣|根|块|勺|汤匙|茶匙)", Pattern.CASE_INSENSITIVE);

    @Value("${fridgeclear.import.howtocook.root:data/source/HowToCook/dishes}")
    private String rootDirectory;
    @Value("${fridgeclear.import.howtocook.repository:https://github.com/Anduin2017/HowToCook.git}")
    private String sourceRepositoryUrl;

    public HowToCookImporter(RecipeSourceDocumentRepository sourceRepository,
                             RecipeRepository recipeRepository,
                             IngredientRepository ingredientRepository,
                             IngredientAliasRepository ingredientAliasRepository,
                             RecipeIngredientRepository recipeIngredientRepository,
                             RecipeStepRepository recipeStepRepository,
                             RecipeMediaRepository recipeMediaRepository) {
        this.sourceRepository = sourceRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientAliasRepository = ingredientAliasRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.recipeStepRepository = recipeStepRepository;
        this.recipeMediaRepository = recipeMediaRepository;
    }

    @Transactional
    public ImportReport importAll() {
        Path root = Path.of(rootDirectory).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("HowToCook 目录不存在: " + root);
        }
        String commit = currentCommit(root.getParent());
        ImportReport report = new ImportReport();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(this::isRecipeFile).sorted().forEach(file -> importOne(file, root, commit, report));
        } catch (IOException exception) {
            throw new IllegalStateException("扫描 HowToCook 目录失败: " + root, exception);
        }
        return report;
    }

    private void importOne(Path file, Path root, String commit, ImportReport report) {
        report.scanned++;
        String relative = root.relativize(file).toString().replace('\\', '/');
        try {
            String raw = Files.readString(file);
            HowToCookParser.ParsedRecipe parsed = parser.parse(file, root, commit, raw);
            RecipeSourceDocument document = sourceRepository.findBySourceRepositoryAndSourcePath(sourceRepositoryUrl, relative)
                    .orElseGet(RecipeSourceDocument::new);
            boolean unchanged = document.getId() != null
                    && parsed.fileHash().equals(document.getFileHash())
                    && HowToCookParser.PARSER_VERSION.equals(document.getParserVersion())
                    && document.getImportStatus() != RecipeEnums.ImportStatus.FAILED
                    && recipeRepository.findBySourceDocumentId(document.getId()).isPresent();
            if (unchanged) { report.skipped++; return; }
            document.setSourceRepository(sourceRepositoryUrl);
            document.setSourceCommit(commit);
            document.setSourcePath(parsed.sourcePath());
            document.setSourceIdentityHash(identityHash(parsed.sourcePath(), commit));
            document.setFileHash(parsed.fileHash());
            document.setRawMarkdown(parsed.rawMarkdown());
            document.setParserVersion(HowToCookParser.PARSER_VERSION);
            document.setImportStatus(parsed.status());
            document.setImportError(parsed.error());
            document.setImportedAt(LocalDateTime.now());
            document = sourceRepository.save(document);
            if (parsed.status() == RecipeEnums.ImportStatus.FAILED) {
                report.failed++;
                report.errors.add(relative + ": " + parsed.error());
                return;
            }

            Recipe recipe = recipeRepository.findBySourceDocumentId(document.getId()).orElseGet(Recipe::new);
            boolean created = recipe.getId() == null;
            recipe.setSourceDocumentId(document.getId());
            recipe.setName(parsed.name());
            recipe.setSlug(slug(parsed.name(), parsed.sourcePath()));
            recipe.setCategory(parsed.category());
            recipe.setDescription(parsed.description());
            recipe.setDifficultyText(parsed.difficultyText());
            recipe.setDifficultyLevel(parsed.difficultyLevel());
            recipe.setCalories(parsed.calories());
            recipe.setSourceTitle(parsed.sourceTitle());
            recipe.setStatus(RecipeEnums.Status.ACTIVE);
            recipe = recipeRepository.save(recipe);
            recipeIngredientRepository.deleteByRecipeId(recipe.getId());
            recipeStepRepository.deleteByRecipeId(recipe.getId());
            recipeMediaRepository.deleteByRecipeId(recipe.getId());
            saveIngredients(recipe.getId(), parsed.ingredients(), report);
            saveSteps(recipe.getId(), parsed.steps());
            saveMedia(recipe.getId(), parsed.media());
            if (created) report.created++; else report.updated++;
            if (parsed.status() == RecipeEnums.ImportStatus.PARTIAL) report.partial++; else report.success++;
        } catch (Exception exception) {
            report.failed++;
            report.errors.add(relative + ": " + exception.getMessage());
        }
    }

    private void saveIngredients(Long recipeId, List<HowToCookParser.ParsedIngredient> parsed, ImportReport report) {
        Map<String, String> quantitiesByName = new HashMap<>();
        parsed.stream().filter(item -> item.sourceSection() == RecipeEnums.SourceSection.CALCULATION)
                .filter(item -> item.rawQuantity() != null && !item.rawQuantity().isBlank())
                .forEach(item -> quantitiesByName.put(normalize(item.rawName()), item.rawQuantity()));
        int order = 0;
        for (HowToCookParser.ParsedIngredient item : parsed) {
            String name = item.rawName().trim();
            if (name.isBlank()) continue;
            String rawQuantity = item.rawQuantity();
            if (rawQuantity == null && item.sourceSection() == RecipeEnums.SourceSection.REQUIRED) {
                rawQuantity = quantitiesByName.get(normalize(name));
            }
            String normalized = normalize(name);
            Ingredient ingredient = resolveIngredient(name).orElseGet(() -> {
                Ingredient created = new Ingredient();
                created.setCanonicalName(name);
                created.setNormalizedName(normalized);
                created.setIngredientType(RecipeEnums.IngredientType.UNKNOWN);
                return ingredientRepository.save(created);
            });
            RecipeIngredient relation = new RecipeIngredient();
            relation.setRecipeId(recipeId);
            relation.setIngredientId(ingredient.getId());
            relation.setRawName(name);
            relation.setRole(roleFor(name));
            relation.setOptional(name.contains("可选"));
            relation.setRawQuantity(rawQuantity);
            parseQuantity(relation, rawQuantity);
            relation.setSourceSection(item.sourceSection());
            relation.setSortOrder(order++);
            recipeIngredientRepository.save(relation);
            if (rawQuantity != null && relation.getQuantityParseStatus() != RecipeEnums.QuantityParseStatus.PARSED) report.unparsedQuantities++;
        }
    }

    private void parseQuantity(RecipeIngredient relation, String rawQuantity) {
        if (rawQuantity == null || rawQuantity.isBlank()) {
            relation.setQuantityParseStatus(RecipeEnums.QuantityParseStatus.UNPARSED);
            return;
        }
        Matcher matcher = QUANTITY.matcher(rawQuantity);
        if (!matcher.find()) {
            relation.setQuantityParseStatus(RecipeEnums.QuantityParseStatus.UNPARSED);
            return;
        }
        relation.setQuantityMin(new java.math.BigDecimal(matcher.group(1)));
        relation.setQuantityMax(new java.math.BigDecimal(matcher.group(2) == null ? matcher.group(1) : matcher.group(2)));
        relation.setUnit(matcher.group(3));
        relation.setQuantityParseStatus(matcher.group(2) == null
                ? RecipeEnums.QuantityParseStatus.PARSED : RecipeEnums.QuantityParseStatus.PARTIAL);
    }

    private void saveSteps(Long recipeId, List<String> steps) {
        for (int index = 0; index < steps.size(); index++) {
            RecipeStep step = new RecipeStep();
            step.setRecipeId(recipeId);
            step.setStepNo(index + 1);
            step.setContent(steps.get(index));
            recipeStepRepository.save(step);
        }
    }

    private void saveMedia(Long recipeId, List<HowToCookParser.ParsedMedia> media) {
        for (HowToCookParser.ParsedMedia item : media) {
            RecipeMedia entity = new RecipeMedia();
            entity.setRecipeId(recipeId);
            entity.setMediaType(RecipeEnums.MediaType.IMAGE);
            entity.setSourcePath(item.sourcePath());
            entity.setAltText(item.altText());
            entity.setSortOrder(item.sortOrder());
            recipeMediaRepository.save(entity);
        }
    }

    private boolean isRecipeFile(Path path) {
        String value = path.toString().replace('\\', '/');
        return Files.isRegularFile(path) && value.endsWith(".md") && !value.contains("/template/")
                && !path.getFileName().toString().equalsIgnoreCase("README.md");
    }

    private String currentCommit(Path repositoryRoot) {
        try {
            Process process = new ProcessBuilder("git", "-C", repositoryRoot.toString(), "rev-parse", "HEAD").start();
            String commit = new String(process.getInputStream().readAllBytes()).trim();
            if (process.waitFor() == 0 && !commit.isBlank()) return commit;
        } catch (Exception ignored) { }
        return "unknown";
    }

    private String normalize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").trim(); }

    private Optional<Ingredient> resolveIngredient(String rawName) {
        String normalized = normalize(rawName);
        return ingredientAliasRepository.findByNormalizedAlias(normalized)
                .flatMap(alias -> ingredientRepository.findById(alias.getIngredientId()))
                .or(() -> ingredientRepository.findByNormalizedName(normalized));
    }

    private RecipeEnums.IngredientRole roleFor(String name) {
        String normalized = normalize(name);
        if (normalized.contains("冰箱") || normalized.contains("燃气灶") || normalized.contains("煤气灶")
                || normalized.contains("电磁炉") || normalized.contains("炒锅") || normalized.contains("平底锅")
                || normalized.contains("平底煎锅") || normalized.contains("厨房用夹") || normalized.contains("夹子")
                || normalized.contains("砂锅") || normalized.contains("铝锅") || normalized.contains("铁锅")
                || normalized.contains("高压锅") || normalized.contains("汤锅") || normalized.contains("烤箱")
                || normalized.contains("电饭煲") || normalized.contains("电炖锅") || normalized.contains("料理机")
                || normalized.contains("搅拌机") || normalized.contains("砧板") || normalized.contains("菜刀")) {
            return RecipeEnums.IngredientRole.TOOL;
        }
        return RecipeEnums.IngredientRole.UNKNOWN;
    }

    private String identityHash(String path, String commit) {
        try {
            byte[] input = (sourceRepositoryUrl + "\n" + commit + "\n" + path).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算源文档身份 hash", exception);
        }
    }

    private String slug(String name, String path) {
        String safe = name.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("^-|-$", "");
        return (safe.isBlank() ? "recipe" : safe) + "-" + Integer.toHexString(path.hashCode());
    }

    public static class ImportReport {
        public int scanned;
        public int success;
        public int partial;
        public int failed;
        public int created;
        public int updated;
        public int skipped;
        public int unparsedQuantities;
        public final List<String> errors = new ArrayList<>();
        public int getScanned() { return scanned; }
        public int getSuccess() { return success; }
        public int getPartial() { return partial; }
        public int getFailed() { return failed; }
        public int getCreated() { return created; }
        public int getUpdated() { return updated; }
        public int getSkipped() { return skipped; }
        public int getUnparsedQuantities() { return unparsedQuantities; }
        public List<String> getErrors() { return errors; }
    }
}
