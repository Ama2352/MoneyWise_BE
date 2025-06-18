package JavaProject.MoneyWise.services;

import JavaProject.MoneyWise.models.dtos.geminiOCR.TransactionInfoDTO;

public interface GeminiService {
    TransactionInfoDTO extractTransactionInfo(String ocrText);
}
