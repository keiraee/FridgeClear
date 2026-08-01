package com.sccothe.fridgeclear.recipe.importer;

import com.sccothe.fridgeclear.recipe.domain.RecipeEnums;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HowToCookParser {
    public static final String PARSER_VERSION = "1.0.0";
    private static final Pattern H1 = Pattern.compile("^#\\s+(.+?)\\s*$", Pattern.MULTILINE);
    private static final Pattern DIFFICULTY = Pattern.compile("预估烹饪难度：\\s*(.+)");
    private static final Pattern CALORIES = Pattern.compile("预估卡路里：\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern ORDERED_STEP = Pattern.compile("^\\s*\\d+[.)]\\s+(.+?)\\s*$");
    private static final Pattern IMAGE = Pattern.compile("!\\[([^]]*)]\\(([^)]+)\\)");
    private static final Pattern QUANTITY = Pattern.compile("(\\d+(?:\\.\\d+)?)(?:\\s*-\\s*(\\d+(?:\\.\\d+)?))?\\s*(克|g|千克|kg|毫升|ml|升|l|个|只|斤|片|瓣|根|块|勺|汤匙|茶匙)", Pattern.CASE_INSENSITIVE);

    public ParsedRecipe parse(Path file, Path dishesRoot, String sourceCommit, String rawMarkdown) {
        String relative = dishesRoot.relativize(file).toString().replace('\\', '/');
        List<String> lines = rawMarkdown.lines().toList();
        Matcher titleMatcher = H1.matcher(rawMarkdown);
        if (!titleMatcher.find()) {
            return ParsedRecipe.failed(relative, sourceCommit, rawMarkdown, sha256(rawMarkdown), "MISSING_TITLE");
        }
        String sourceTitle = titleMatcher.group(1).trim();
        String name = sourceTitle.replaceFirst("的做法$", "").trim();
        Map<String, String> sections = sections(lines);
        String description = description(lines);
        String difficultyText = findValue(rawMarkdown, DIFFICULTY);
        Byte difficultyLevel = difficultyLevel(difficultyText);
        BigDecimal calories = decimal(findValue(rawMarkdown, CALORIES));
        List<ParsedIngredient> ingredients = new ArrayList<>();
        ingredients.addAll(parseRequired(sections.get("必备原料和工具")));
        ingredients.addAll(parseCalculation(sections.get("计算")));
        List<String> steps = parseSteps(sections.get("操作"));
        List<ParsedMedia> media = parseMedia(rawMarkdown, file.getParent(), dishesRoot);
        RecipeEnums.Category category = category(relative);
        RecipeEnums.ImportStatus status = name.isBlank() || ingredients.isEmpty() || steps.isEmpty()
                ? RecipeEnums.ImportStatus.FAILED
                : (difficultyText == null || calories == null || sections.get("计算") == null
                ? RecipeEnums.ImportStatus.PARTIAL : RecipeEnums.ImportStatus.SUCCESS);
        return new ParsedRecipe(relative, sourceCommit, sha256(rawMarkdown), rawMarkdown, name, sourceTitle,
                category, description, difficultyText, difficultyLevel, calories, ingredients, steps, media, status,
                status == RecipeEnums.ImportStatus.FAILED ? "MISSING_REQUIRED_CONTENT" : null);
    }

    private Map<String, String> sections(List<String> lines) {
        Map<String, StringBuilder> values = new LinkedHashMap<>();
        String current = null;
        for (String line : lines) {
            if (line.startsWith("## ")) {
                current = line.substring(3).trim();
                values.putIfAbsent(current, new StringBuilder());
            } else if (current != null) {
                values.get(current).append(line).append('\n');
            }
        }
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(key, value.toString().trim()));
        return result;
    }

    private String description(List<String> lines) {
        StringBuilder result = new StringBuilder();
        boolean afterTitle = false;
        for (String line : lines) {
            if (line.startsWith("# ")) { afterTitle = true; continue; }
            if (line.startsWith("## ")) break;
            if (afterTitle && !line.isBlank() && !line.startsWith("![") && !line.startsWith("预估")) {
                if (result.length() > 0) result.append('\n');
                result.append(line.trim());
            }
        }
        return result.toString();
    }

    private List<ParsedIngredient> parseRequired(String section) {
        if (section == null) return List.of();
        List<ParsedIngredient> result = new ArrayList<>();
        for (String line : section.lines().toList()) {
            String value = listValue(line);
            if (value != null) result.add(new ParsedIngredient(cleanName(value), null, RecipeEnums.SourceSection.REQUIRED));
        }
        return result;
    }

    private List<ParsedIngredient> parseCalculation(String section) {
        if (section == null) return List.of();
        List<ParsedIngredient> result = new ArrayList<>();
        for (String line : section.lines().toList()) {
            String value = listValue(line);
            if (value == null) continue;
            String[] equal = value.split("=", 2);
            String rawName = equal.length == 2 ? equal[0].trim() : nameBeforeQuantity(value);
            String rawQuantity = equal.length == 2 ? equal[1].trim() : value.substring(Math.min(rawName.length(), value.length())).trim();
            result.add(new ParsedIngredient(cleanName(rawName), rawQuantity, RecipeEnums.SourceSection.CALCULATION));
        }
        return result;
    }

    private List<String> parseSteps(String section) {
        if (section == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String line : section.lines().toList()) {
            Matcher matcher = ORDERED_STEP.matcher(line);
            if (matcher.find()) result.add(matcher.group(1).trim());
        }
        return result;
    }

    private List<ParsedMedia> parseMedia(String raw, Path fileParent, Path root) {
        List<ParsedMedia> result = new ArrayList<>();
        Matcher matcher = IMAGE.matcher(raw);
        while (matcher.find()) {
            String path = fileParent.resolve(matcher.group(2)).normalize().toString();
            result.add(new ParsedMedia(root.relativize(Path.of(path)).toString().replace('\\', '/'), matcher.group(1), result.size()));
        }
        return result;
    }

    private String listValue(String line) {
        String value = line.trim();
        if (value.startsWith("- ") || value.startsWith("+ ") || value.startsWith("* ")) {
            return value.substring(2).trim();
        }
        return null;
    }

    private String nameBeforeQuantity(String value) {
        Matcher matcher = QUANTITY.matcher(value);
        return matcher.find() ? value.substring(0, matcher.start()).trim() : value;
    }

    private String cleanName(String value) {
        return value.replaceAll("（可选）|\\(可选\\)", "").replaceFirst("\\s*[（(]别称：.*?[）)]", "").trim();
    }

    private String findValue(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private Byte difficultyLevel(String value) {
        if (value == null) return null;
        if (value.contains("简单") && !value.contains("较")) return 1;
        if (value.contains("较简单")) return 2;
        if (value.contains("中等")) return 3;
        if (value.contains("较难")) return 4;
        if (value.contains("困难")) return 5;
        int stars = (int) value.chars().filter(c -> c == '★').count();
        return stars == 0 ? null : (byte) stars;
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private RecipeEnums.Category category(String relativePath) {
        String first = relativePath.split("/")[0];
        return switch (first) {
            case "aquatic" -> RecipeEnums.Category.AQUATIC;
            case "breakfast" -> RecipeEnums.Category.BREAKFAST;
            case "condiment" -> RecipeEnums.Category.CONDIMENT;
            case "dessert" -> RecipeEnums.Category.DESSERT;
            case "drink" -> RecipeEnums.Category.DRINK;
            case "meat_dish" -> RecipeEnums.Category.MEAT_DISH;
            case "semi-finished" -> RecipeEnums.Category.SEMI_FINISHED;
            case "soup" -> RecipeEnums.Category.SOUP;
            case "staple" -> RecipeEnums.Category.STAPLE;
            case "vegetable_dish" -> RecipeEnums.Category.VEGETABLE_DISH;
            default -> RecipeEnums.Category.UNKNOWN;
        };
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算 Markdown hash", exception);
        }
    }

    public record ParsedIngredient(String rawName, String rawQuantity, RecipeEnums.SourceSection sourceSection) {}
    public record ParsedMedia(String sourcePath, String altText, int sortOrder) {}
    public record ParsedRecipe(String sourcePath, String sourceCommit, String fileHash, String rawMarkdown,
                               String name, String sourceTitle, RecipeEnums.Category category, String description,
                               String difficultyText, Byte difficultyLevel, BigDecimal calories,
                               List<ParsedIngredient> ingredients, List<String> steps, List<ParsedMedia> media,
                               RecipeEnums.ImportStatus status, String error) {
        static ParsedRecipe failed(String path, String commit, String raw, String hash, String error) {
            return new ParsedRecipe(path, commit, hash, raw, "", "", RecipeEnums.Category.UNKNOWN, "", null, null,
                    null, List.of(), List.of(), List.of(), RecipeEnums.ImportStatus.FAILED, error);
        }
    }
}
