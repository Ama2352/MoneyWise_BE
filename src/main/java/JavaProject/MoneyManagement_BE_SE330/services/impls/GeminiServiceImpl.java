package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.geminiOCR.TransactionInfoDTO;
import JavaProject.MoneyManagement_BE_SE330.services.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.endpoint}")
    private String endpoint;

    public TransactionInfoDTO extractTransactionInfo(String ocrText) {
        try {
            // Construct the prompt
            String prompt = """
                You are a financial data extraction assistant. Your job is to extract structured transaction information from the OCR text below.

                Please extract ONLY the following fields:
                - transactionId: A unique transaction identifier (string)
                - amount: The transaction amount as a decimal number (no currency symbols)
                - date: The transaction date in ISO 8601 format (yyyy-MM-dd)
                - bankName: The full name of the bank (string)

                OCR text:
                %s

                Return ONLY a valid JSON object with these exact keys and no additional formatting, markdown, comments, or explanation.
                Do NOT wrap the result in triple backticks or any other formatting.

                Example output:
                {
                  "transactionId": "TXN123456789",
                  "amount": 125.50,
                  "date": "2023-05-15",
                  "bankName": "First National Bank"
                }
                """.formatted(ocrText);

            // Prepare the request body
            String requestBody = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "%s"
                        }
                      ]
                    }
                  ],
                  "generationConfig": {
                    "responseMimeType": "application/json"
                  }
                }
                """.formatted(prompt.replace("\n", "\\n").replace("\"", "\\\""));

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            // Call Gemini API
            String url = endpoint + "?key=" + apiKey;
            String response = restTemplate.postForObject(url, request, String.class);

            // Parse the response
            TransactionInfoDTO result = objectMapper.readValue(
                    objectMapper.readTree(response).path("candidates").get(0).path("content").path("parts").get(0).path("text").asText(),
                    TransactionInfoDTO.class
            );

            return result;
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            throw new RuntimeException("Failed to extract transaction info: " + e.getMessage());
        }
    }
}