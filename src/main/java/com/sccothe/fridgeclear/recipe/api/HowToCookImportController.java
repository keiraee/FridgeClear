package com.sccothe.fridgeclear.recipe.api;

import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.recipe.importer.HowToCookImporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/imports")
@Tag(name = "Recipe Import", description = "HowToCook 菜谱导入")
public class HowToCookImportController {
    private final HowToCookImporter importer;

    public HowToCookImportController(HowToCookImporter importer) { this.importer = importer; }

    @PostMapping("/howtocook")
    @Operation(summary = "导入 HowToCook 菜谱")
    public ApiResponse<HowToCookImporter.ImportReport> importHowToCook() {
        return ApiResponse.success(importer.importAll(), "import");
    }
}
