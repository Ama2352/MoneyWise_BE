package JavaProject.MoneyManagement_BE_SE330.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME, // e.g., 2025-05-20T12:40:18
            DateTimeFormatter.ISO_OFFSET_DATE_TIME, // e.g., 2025-05-20T12:40:18+00:00
            DateTimeFormatter.ISO_ZONED_DATE_TIME, // e.g., 2025-05-20T12:40:18Z
            DateTimeFormatter.ISO_DATE_TIME // e.g., 2025-05-20T12:40:18.333Z
    );

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().trim();
        if (value.isEmpty()) {
            throw new IOException("Date string is empty");
        }

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Continue trying other formatters
            }
        }
        throw new IOException("Failed to parse date: '" + value + "'. Supported formats: " + FORMATTERS);
    }
}