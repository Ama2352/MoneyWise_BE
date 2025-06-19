package JavaProject.MoneyWise.services;

import JavaProject.MoneyWise.models.dtos.transaction.*;
import java.util.List;
import java.util.UUID;

public interface TransactionService {
    List<TransactionDTO> getAllTransactions();
    List<TransactionDTO> getTransactionsByWalletId(UUID walletId);
    TransactionDTO getTransactionById(UUID transactionId);
    TransactionDTO createTransaction(CreateTransactionDTO model);
    TransactionDTO updateTransaction(UpdateTransactionDTO model);
    UUID deleteTransactionById(UUID transactionId);
    List<TransactionDetailDTO> searchTransactions(SearchTransactionsDTO filter);
}
