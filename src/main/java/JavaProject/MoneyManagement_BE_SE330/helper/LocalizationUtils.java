package JavaProject.MoneyManagement_BE_SE330.helper;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for managing localized seed data
 * Coordinates with frontend's Accept-Language header implementation
 */
public class LocalizationUtils {
    
    /**
     * Default English category names
     */
    public static final List<String> DEFAULT_ENGLISH_CATEGORIES = Arrays.asList(
        "Food & Dining",
        "Transportation",
        "Entertainment",
        "Housing",
        "Utilities",
        "Shopping",
        "Salary",
        "Freelance",
        "Gifts",
        "Investments"
    );


    /**
     * Default Vietnamese category names
     */
    public static final List<String> DEFAULT_VIETNAMESE_CATEGORIES = Arrays.asList(
        "Ăn uống",
        "Di chuyển",
        "Giải trí",
        "Nhà ở",
        "Tiện ích",
        "Mua sắm",
        "Lương",
        "Làm tự do",
        "Quà tặng",
        "Đầu tư"
    );
    
    /**
     * Default English wallet names
     */
    public static final List<String> DEFAULT_ENGLISH_WALLETS = Arrays.asList(
        "Cash",
        "Bank Account", 
        "Credit Card"
    );
    
    /**
     * Default Vietnamese wallet names
     */
    public static final List<String> DEFAULT_VIETNAMESE_WALLETS = Arrays.asList(
        "Tiền mặt",
        "Tài khoản ngân hàng",
        "Thẻ tín dụng"
    );
    
    /**
     * Determines if the Accept-Language header indicates Vietnamese language preference
     * @param acceptLanguageHeader the Accept-Language header value
     * @return true if Vietnamese is preferred, false for English (default)
     */
    public static boolean isVietnamese(String acceptLanguageHeader) {
        if (acceptLanguageHeader == null || acceptLanguageHeader.trim().isEmpty()) {
            return false; // Default to English
        }
        
        // Check if Vietnamese language codes are present
        String normalized = acceptLanguageHeader.toLowerCase();
        return normalized.contains("vi-vn") || normalized.contains("vi");
    }
    
    /**
     * Gets localized category names based on language preference
     * @param isVietnamese true for Vietnamese, false for English
     * @return list of localized category names
     */
    public static List<String> getLocalizedCategories(boolean isVietnamese) {
        return isVietnamese ? DEFAULT_VIETNAMESE_CATEGORIES : DEFAULT_ENGLISH_CATEGORIES;
    }
    
    /**
     * Gets localized wallet names based on language preference
     * @param isVietnamese true for Vietnamese, false for English
     * @return list of localized wallet names
     */
    public static List<String> getLocalizedWallets(boolean isVietnamese) {
        return isVietnamese ? DEFAULT_VIETNAMESE_WALLETS : DEFAULT_ENGLISH_WALLETS;
    }
    
    /**
     * Gets a specific localized category name by index
     * @param index the index of the category
     * @param isVietnamese true for Vietnamese, false for English
     * @return localized category name or null if index is out of bounds
     */
    public static String getLocalizedCategoryName(int index, boolean isVietnamese) {
        List<String> categories = getLocalizedCategories(isVietnamese);
        if (index >= 0 && index < categories.size()) {
            return categories.get(index);
        }
        return null;
    }
    
    /**
     * Gets a specific localized wallet name by index
     * @param index the index of the wallet
     * @param isVietnamese true for Vietnamese, false for English
     * @return localized wallet name or null if index is out of bounds
     */
    public static String getLocalizedWalletName(int index, boolean isVietnamese) {
        List<String> wallets = getLocalizedWallets(isVietnamese);
        if (index >= 0 && index < wallets.size()) {
            return wallets.get(index);
        }
        return null;
    }
}
