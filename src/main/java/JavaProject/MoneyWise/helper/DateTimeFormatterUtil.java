package JavaProject.MoneyWise.helper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class DateTimeFormatterUtil {

    /**
     * Formats a LocalDateTime to a locale-specific, user-friendly date and time string.
     *
     * @param dateTime       The LocalDateTime to format (e.g., 2025-06-30T07:33:00).
     * @param acceptLanguage The language code (e.g., "vi-VN", "en-US"). Defaults to "vi-VN" if null or empty.
     * @param includeTime    Whether to include time in the output (e.g., "07:33" or "7:33 AM").
     * @return A formatted string (e.g., "7:33 AM, June 30, 2025" for en-US with time, "June 30, 2025" without).
     * @throws IllegalArgumentException if dateTime is null.
     */
    public static String formatDateTimeWithLanguage(LocalDateTime dateTime, String acceptLanguage, boolean includeTime) {
        // Validate input
        if (dateTime == null) {
            throw new IllegalArgumentException("DateTime cannot be null");
        }

        // Default to Vietnamese if acceptLanguage is null or empty
        String language = (acceptLanguage == null || acceptLanguage.isEmpty()) ? "vi-VN" : acceptLanguage;

        // Parse the language code into a Locale
        Locale locale;
        try {
            locale = Locale.forLanguageTag(language);
            if (locale.getLanguage().isEmpty()) {
                locale = Locale.of("vi"); // Fallback to Vietnamese
            }
        } catch (Exception e) {
            locale = Locale.of("vi"); // Fallback to Vietnamese on invalid language code
        }

        // Define locale-specific patterns
        String datePattern;
        String timePattern = includeTime ? "HH:mm, " : ""; // 24-hour format by default with comma
        if (locale.getLanguage().equals("vi")) {
            datePattern = "d MMMM yyyy"; // e.g., "30 Tháng Sáu 2025"
        } else if (locale.getLanguage().equals("en") && locale.getCountry().equals("US")) {
            datePattern = "MMMM d, yyyy"; // e.g., "June 30, 2025"
            timePattern = includeTime ? "h:mm a, " : ""; // 12-hour with AM/PM, e.g., "7:33 AM, "
        } else {
            // Fallback to localized medium style for date, short for time
            DateTimeFormatter formatter = includeTime
                    ? DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
                    : DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);
            return formatter.format(dateTime);
        }

        // Combine time and date patterns (time before date)
        String pattern = timePattern + datePattern;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, locale);
        return formatter.format(dateTime);
    }

    /**
     * Formats a LocalDate to a locale-specific, user-friendly date string.
     *
     * @param date           The LocalDate to format (e.g., 2025-06-30).
     * @param acceptLanguage The language code (e.g., "vi-VN", "en-US"). Defaults to "vi-VN" if null or empty.
     * @return A formatted date string (e.g., "June 30, 2025" for en-US, "30 Tháng Sáu 2025" for vi-VN).
     * @throws IllegalArgumentException if date is null.
     */
    public static String formatDateWithLanguage(LocalDate date, String acceptLanguage) {
        // Validate input
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        // Default to Vietnamese if acceptLanguage is null or empty
        String language = (acceptLanguage == null || acceptLanguage.isEmpty()) ? "vi-VN" : acceptLanguage;

        // Parse the language code into a Locale
        Locale locale;
        try {
            locale = Locale.forLanguageTag(language);
            if (locale.getLanguage().isEmpty()) {
                locale = Locale.of("vi"); // Fallback to Vietnamese
            }
        } catch (Exception e) {
            locale = Locale.of("vi"); // Fallback to Vietnamese on invalid language code
        }

        // Define locale-specific patterns
        String pattern;
        if (locale.getLanguage().equals("vi")) {
            pattern = "d MMMM yyyy"; // e.g., "30 Tháng Sáu 2025"
        } else if (locale.getLanguage().equals("en") && locale.getCountry().equals("US")) {
            pattern = "MMMM d, yyyy"; // e.g., "June 30, 2025"
        } else {
            // Fallback to localized medium style
            return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, locale);
        return formatter.format(date);
    }
}