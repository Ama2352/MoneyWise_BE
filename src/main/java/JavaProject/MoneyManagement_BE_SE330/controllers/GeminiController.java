package JavaProject.MoneyManagement_BE_SE330.controllers;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.geminiOCR.OcrTextRequestDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.geminiOCR.TransactionInfoDTO;
import JavaProject.MoneyManagement_BE_SE330.services.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {
    private final GeminiService geminiService;

    @PostMapping("/extract-ocr")
    public ResponseEntity<TransactionInfoDTO> extractTransaction(@RequestBody OcrTextRequestDTO request) {
        try {
            if (request.getOcrText() == null || request.getOcrText().isEmpty()) {
                throw new IllegalArgumentException("OCR text is required");
            }
            // Escape control characters
            String cleanedOcrText = request.getOcrText()
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            TransactionInfoDTO transactionInfo = geminiService.extractTransactionInfo(cleanedOcrText);
            return ResponseEntity.ok(transactionInfo);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid request: " + e.getMessage());
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to extract transaction info: " + e.getMessage());
        }
    }
}