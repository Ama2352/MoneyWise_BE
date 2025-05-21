package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.CreateTransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.TransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.UpdateTransactionDTO;

import java.util.List;
import java.util.UUID;

public interface TransactionService {
    List<TransactionDTO> getAllTransactions();
    List<TransactionDTO> getTransactionsByWalletId(UUID walletId);
    TransactionDTO getTransactionById(UUID transactionId);
    TransactionDTO createTransaction(CreateTransactionDTO model);
    TransactionDTO updateTransaction(UpdateTransactionDTO model);
    UUID deleteTransactionById(UUID transactionId);
}
