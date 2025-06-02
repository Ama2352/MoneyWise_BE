package JavaProject.MoneyManagement_BE_SE330.controllers;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.category.*;
import JavaProject.MoneyManagement_BE_SE330.services.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/Categories")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Category")
public class CategoryController {
    private final CategoryService categoryService;
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        List<CategoryDTO> categories = categoryService.getAllCategories(acceptLanguage);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable("categoryId") UUID categoryId) {
        CategoryDTO found = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(found);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<UUID> deleteCategory(@PathVariable("categoryId") UUID categoryId) {
        UUID deletedId = categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(deletedId);
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody @Valid CreateCategoryDTO model) {
        CategoryDTO created = categoryService.createCategory(model);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<CategoryDTO> updateCategory(@RequestBody @Valid UpdateCategoryDTO model) {
        CategoryDTO updated = categoryService.updateCategory(model);
        return ResponseEntity.ok(updated);
    }
}
