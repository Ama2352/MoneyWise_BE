package JavaProject.MoneyWise.helper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class DateTimeFormatterUtil {

    /**
     * Formats a LocalDateTime to a locale-specific date format with hyphens.
     *
     * @param dateTime       The LocalDateTime to format (e.g., 2025-06-07T14:54:51).
     * @param acceptLanguage The language code (e.g., "vi-VN", "en-US"). If null or empty, defaults to "vi-VN".
     * @return A formatted date string in the locale-specific format (e.g., "07-06-2025" for vi-VN, "06-07-2025" for en-US).
     * @throws IllegalArgumentException if dateTime is null.
     */
    public static String formatDateTimeWithLanguage(LocalDateTime dateTime, String acceptLanguage) {
        // Validate input
        if (dateTime == null) {
            throw new IllegalArgumentException("DateTime cannot be null");
        }

        // Extract LocalDate
        LocalDate date = dateTime.toLocalDate();

        // Default to Vietnamese if acceptLanguage is null or empty
        String language = (acceptLanguage == null || acceptLanguage.isEmpty()) ? "vi-VN" : acceptLanguage;

        // Parse the language code into a Locale
        Locale locale;
        try {
            locale = Locale.forLanguageTag(language);
            // Validate that the locale is supported
            if (locale.getLanguage().isEmpty()) {
                locale = Locale.of("vi"); // Fallback to Vietnamese
            }
        } catch (Exception e) {
            locale = Locale.of("vi"); // Fallback to Vietnamese on invalid language code
        }

        // Define locale-specific date patterns with hyphens
        String pattern;
        if (locale.getLanguage().equals("vi")) {
            pattern = "dd-MM-yyyy"; // Day-first for Vietnamese
        } else if (locale.getLanguage().equals("en") && locale.getCountry().equals("US")) {
            pattern = "MM-dd-yyyy"; // Month-first for US English
        } else {
            // Use medium format style as a fallback for other locales
            return DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(locale)
                    .format(date);
        }

        // Format the date using the specified pattern
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(pattern, locale);
        return outputFormatter.format(date);
    }
}