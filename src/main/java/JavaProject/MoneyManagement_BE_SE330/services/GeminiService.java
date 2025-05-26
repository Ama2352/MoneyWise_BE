package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.geminiOCR.TransactionInfoDTO;

public interface GeminiService {
    TransactionInfoDTO extractTransactionInfo(String ocrText);
}
