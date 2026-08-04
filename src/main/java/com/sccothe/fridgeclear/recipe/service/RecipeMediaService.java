package com.sccothe.fridgeclear.recipe.service;

import com.sccothe.fridgeclear.common.api.ResourceNotFoundException;
import com.sccothe.fridgeclear.recipe.domain.RecipeMedia;
import com.sccothe.fridgeclear.recipe.repository.RecipeMediaQueryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class RecipeMediaService {
    private final RecipeMediaQueryRepository repository;
    private final Path mediaRoot;

    public RecipeMediaService(
            RecipeMediaQueryRepository repository,
            @Value("${fridgeclear.recipe.media-root:data/source/HowToCook/dishes}") String mediaRoot
    ) {
        this.repository = repository;
        this.mediaRoot = Path.of(mediaRoot).toAbsolutePath().normalize();
    }

    public MediaFile load(Long recipeId, Integer sortOrder) {
        RecipeMedia media = repository.findByRecipeIdOrderBySortOrderAsc(recipeId).stream()
                .filter(item -> item.getSortOrder().equals(sortOrder))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("菜谱图片不存在"));

        Path file = mediaRoot.resolve(media.getSourcePath()).normalize();
        if (!file.startsWith(mediaRoot) || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("菜谱图片文件不存在");
        }
        try {
            String contentType = Files.probeContentType(file);
            MediaType mediaType = contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);
            return new MediaFile(new FileSystemResource(file), mediaType, media.getAltText());
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("读取菜谱图片失败", exception);
        }
    }

    public record MediaFile(Resource resource, MediaType mediaType, String altText) {}
}
