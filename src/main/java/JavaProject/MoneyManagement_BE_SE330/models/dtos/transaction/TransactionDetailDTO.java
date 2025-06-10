package JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailDTO {

    private UUID transactionID;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transactionDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    private String time = "";
    private String dayOfWeek = "";
    private String month = "";
    private BigDecimal amount;
    private String type = "";
    private String categoryName = "";
    private UUID categoryID;
    private String description;
    private UUID walletID;
    private String walletName = "";
}
