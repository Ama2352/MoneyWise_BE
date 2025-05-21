package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.*;

import java.time.LocalDate;
import java.time.YearMonth;
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
    List<CategoryBreakdownDTO> getCategoryBreakdown(LocalDate startDate, LocalDate endDate);
    DailySummaryDTO getDailySummary(LocalDate date);
    WeeklySummaryDTO getWeeklySummary(LocalDate weekStartDate);
    MonthlySummaryDTO getMonthlySummary(YearMonth yearMonth);
    YearlySummaryDTO getYearlySummary(int year);
}
