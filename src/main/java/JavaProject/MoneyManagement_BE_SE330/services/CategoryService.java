package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.category.*;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    List<CategoryDTO> getAllCategories();
    CategoryDTO getCategoryById(UUID categoryId);
    CategoryDTO createCategory(CreateCategoryDTO model);
    CategoryDTO updateCategory(UpdateCategoryDTO model);
    UUID deleteCategory(UUID categoryId);
}
