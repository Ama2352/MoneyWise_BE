package JavaProject.MoneyWise.services;

import JavaProject.MoneyWise.models.dtos.category.*;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    List<CategoryDTO> getAllCategories();
    List<CategoryDTO> getAllCategories(String acceptLanguage);
    CategoryDTO getCategoryById(UUID categoryId);
    CategoryDTO createCategory(CreateCategoryDTO model);
    CategoryDTO updateCategory(UpdateCategoryDTO model);
    UUID deleteCategory(UUID categoryId);
}
