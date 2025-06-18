package JavaProject.MoneyWise.services.impls;

import JavaProject.MoneyWise.helper.ApplicationMapper;
import JavaProject.MoneyWise.helper.HelperFunctions;
import JavaProject.MoneyWise.helper.LocalizationUtils;
import JavaProject.MoneyWise.helper.ResourceNotFoundException;
import JavaProject.MoneyWise.models.dtos.category.*;
import JavaProject.MoneyWise.models.entities.Category;
import JavaProject.MoneyWise.models.entities.User;
import JavaProject.MoneyWise.repositories.CategoryRepository;
import JavaProject.MoneyWise.repositories.UserRepository;
import JavaProject.MoneyWise.services.CategoryService;
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
        Category category = categoryRepository.findById(model.getCategoryId())
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
