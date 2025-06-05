package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.*;
import java.util.List;
import java.util.UUID;

public interface TransactionService {
    List<TransactionDTO> getAllTransactions();
    List<TransactionDTO> getTransactionsByWalletId(UUID walletId);
    TransactionDTO getTransactionById(UUID transactionId);
    TransactionDTO createTransaction(CreateTransactionDTO model);
    TransactionDTO updateTransaction(UpdateTransactionDTO model);
    UUID deleteTransactionById(UUID transactionId);
    List<TransactionDetailDTO> getTransactionsByDateRange(GetTransactionsByDateRangeDTO filter);
    List<TransactionDetailDTO> searchTransactions(SearchTransactionsDTO filter);
}
