package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.helper.LocalizationUtils;
import JavaProject.MoneyManagement_BE_SE330.helper.ResourceNotFoundException;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.category.*;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Category;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.repositories.CategoryRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;

    @Override
    public List<CategoryDTO> getAllCategories() {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        return categoryRepository.findAllByUser(currentUser)
                .stream()
                .map(applicationMapper::toCategoryDTO)
                .toList();
    }

    @Override
    public List<CategoryDTO> getAllCategories(String acceptLanguage) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        List<Category> userCategories = categoryRepository.findAllByUser(currentUser);
        
        // If user has no categories yet, create localized seed categories
        if (userCategories.isEmpty()) {
            return createLocalizedSeedCategories(currentUser, acceptLanguage);
        }
        
        // Return existing categories (they were created with localized names)
        return userCategories.stream()
                .map(applicationMapper::toCategoryDTO)
                .toList();
    }
    
    private List<CategoryDTO> createLocalizedSeedCategories(User user, String acceptLanguage) {
        boolean isVietnamese = LocalizationUtils.isVietnamese(acceptLanguage);
        List<String> localizedNames = LocalizationUtils.getLocalizedCategories(isVietnamese);
        
        List<Category> categories = localizedNames.stream()
                .map(name -> new Category(name, user))
                .toList();
        
        categoryRepository.saveAll(categories);
        
        return categories.stream()
                .map(applicationMapper::toCategoryDTO)
                .toList();
    }


    @Override
    public CategoryDTO getCategoryById(UUID categoryId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Category category = categoryRepository.findByCategoryIdAndUser(categoryId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return applicationMapper.toCategoryDTO(category);
    }


    @Override
    public CategoryDTO createCategory(CreateCategoryDTO model) {
        Category category = applicationMapper.toCategoryEntity(model);
        category.setUser(HelperFunctions.getCurrentUser(userRepository));
        categoryRepository.save(category);
        return applicationMapper.toCategoryDTO(category);
    }

    @Override
    public CategoryDTO updateCategory(UpdateCategoryDTO model) {
        Category category = categoryRepository.findById(model.getCategoryID())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setUser(HelperFunctions.getCurrentUser(userRepository));
        category.setName(model.getName());
        categoryRepository.save(category);
        return applicationMapper.toCategoryDTO(category);
    }

    @Override
    public UUID deleteCategory(UUID categoryId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Category category = categoryRepository.findByCategoryIdAndUser(categoryId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        categoryRepository.delete(category);
        return categoryId;
    }

}
