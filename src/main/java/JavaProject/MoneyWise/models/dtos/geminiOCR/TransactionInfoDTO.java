package JavaProject.MoneyWise.models.dtos.geminiOCR;

import lombok.Data;

@Data
public class TransactionInfoDTO {
    private String transactionId;
    private double amount;
    private String date;
    private String bankName;
}