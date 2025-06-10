package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.helper.ResourceNotFoundException;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.*;
import JavaProject.MoneyManagement_BE_SE330.models.entities.*;
import JavaProject.MoneyManagement_BE_SE330.repositories.*;
import JavaProject.MoneyManagement_BE_SE330.services.SavingGoalService;
import JavaProject.MoneyManagement_BE_SE330.services.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final ApplicationMapper applicationMapper;
    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;
    private final SavingGoalRepository savingGoalRepository;
    private final SavingGoalService savingGoalService;

    // --- Khai báo hằng số để tránh hardcode chuỗi và dễ bảo trì ---
    private static final String TRANSACTION_TYPE_INCOME = "income";
    private static final String TRANSACTION_TYPE_EXPENSE = "expense";


    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> getAllTransactions() {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        return transactionRepository.findAllByWalletUser(currentUser)
                .stream()
                .map(applicationMapper::toTransactionDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByWalletId(UUID walletId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to this wallet's transactions");
        }
        return transactionRepository.findByWalletWalletId(walletId)
                .stream()
                .map(applicationMapper::toTransactionDTO)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public TransactionDTO getTransactionById(UUID transactionId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to this transaction");
        }

        return applicationMapper.toTransactionDTO(transaction);
    }

    @Override
    @Transactional
    public TransactionDTO createTransaction(CreateTransactionDTO model) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);

        Wallet wallet = walletRepository.findById(model.getWalletID())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if(!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this wallet");
        }

        Category category = categoryRepository.findById(model.getCategoryID())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if(!category.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this category");
        }

        Transaction transaction = applicationMapper.toTransactionEntity(model);
        transaction.setWallet(wallet);
        transaction.setCategory(category);

        transactionRepository.save(transaction);

        // Update UserBudget for Expense transactions
        if (TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(transaction.getType())) {
            List<Budget> budgets = budgetRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    transaction.getCategory(),
                    transaction.getWallet(),
                    transaction.getTransactionDate(),
                    transaction.getTransactionDate()
            );
            if (!budgets.isEmpty()) {
                budgetRepository.updateCurrentSpending(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getAmount(),
                        transaction.getTransactionDate()
                );
            }
        }

        // Update UserSavingGoal for Income transactions
        if (TRANSACTION_TYPE_INCOME.equalsIgnoreCase(transaction.getType())) {
            List<SavingGoal> goals = savingGoalRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    transaction.getCategory(),
                    transaction.getWallet(),
                    transaction.getTransactionDate(),
                    transaction.getTransactionDate()
            );
            if (!goals.isEmpty()) {
                savingGoalRepository.updateSavedAmount(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getAmount(),
                        transaction.getTransactionDate()
                );
                // Refresh saving goals to update savedPercentage
                savingGoalService.getSavingGoalProgressAndAlerts();
            }
        }

        // Update Wallet Balance
        BigDecimal balanceChange = TRANSACTION_TYPE_INCOME.equalsIgnoreCase(transaction.getType())
                ? transaction.getAmount()
                : transaction.getAmount().negate();
        walletRepository.updateBalance(transaction.getWallet().getWalletId(), balanceChange);

        return applicationMapper.toTransactionDTO(transaction);
    }

    @Override
    @Transactional
    public TransactionDTO updateTransaction(UpdateTransactionDTO model) {
        // Fetch existing transaction
        Transaction transaction = transactionRepository.findById(model.getTransactionID())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        if (!transaction.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to update this transaction");
        }

        // Fetch Wallet and Category for the updated values
        Wallet wallet = walletRepository.findById(model.getWalletID())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You cannot move transaction to a wallet you don't own");
        }

        Category category = categoryRepository.findById(model.getCategoryID())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You cannot assign a category you don't own");
        }

        // Check if relevant fields have changed
        boolean hasChanges = !Objects.equals(transaction.getAmount(), model.getAmount()) ||
                !Objects.equals(transaction.getCategory().getCategoryId(), model.getCategoryID()) ||
                !Objects.equals(transaction.getWallet().getWalletId(), model.getWalletID()) ||
                !Objects.equals(transaction.getTransactionDate(), model.getTransactionDate());

        // Step 1: Reverse original effects if relevant fields changed
        if (hasChanges) {
            if (TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(transaction.getType())) {
                List<Budget> budgets = budgetRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getTransactionDate(),
                        transaction.getTransactionDate()
                );
                if (!budgets.isEmpty()) {
                    budgetRepository.updateCurrentSpending(
                            transaction.getCategory(),
                            transaction.getWallet(),
                            transaction.getAmount().negate(), // Reverse the amount
                            transaction.getTransactionDate()
                    );
                }
            } else if (TRANSACTION_TYPE_INCOME.equalsIgnoreCase(transaction.getType())) {
                List<SavingGoal> goals = savingGoalRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getTransactionDate(),
                        transaction.getTransactionDate()
                );
                if (!goals.isEmpty()) {
                    savingGoalRepository.updateSavedAmount(
                            transaction.getCategory(),
                            transaction.getWallet(),
                            transaction.getAmount().negate(), // Reverse the amount
                            transaction.getTransactionDate()
                    );
                    // Refresh saving goals to update savedPercentage
                    savingGoalService.getSavingGoalProgressAndAlerts();
                }
            }

            // Reverse Wallet Balance
            BigDecimal originalBalanceChange = TRANSACTION_TYPE_INCOME.equalsIgnoreCase(transaction.getType())
                    ? transaction.getAmount().negate() // Undo income
                    : transaction.getAmount(); // Undo expense
            walletRepository.updateBalance(transaction.getWallet().getWalletId(), originalBalanceChange);
        }

        // Step 2: Update transaction fields (excluding Type)
        transaction.setAmount(model.getAmount());
        transaction.setDescription(model.getDescription());
        transaction.setTransactionDate(model.getTransactionDate());
        transaction.setWallet(wallet);
        transaction.setCategory(category);

        // Save updated transaction
        Transaction updatedTransaction = transactionRepository.save(transaction);

        // Step 3: Apply new effects if relevant fields changed
        if (hasChanges) {
            BigDecimal newBalanceChange;
            if (TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(transaction.getType())) {
                List<Budget> budgets = budgetRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getTransactionDate(),
                        transaction.getTransactionDate()
                );
                if (!budgets.isEmpty()) {
                    budgetRepository.updateCurrentSpending(
                            transaction.getCategory(),
                            transaction.getWallet(),
                            transaction.getAmount(),
                            transaction.getTransactionDate()
                    );
                }
                newBalanceChange = transaction.getAmount().negate(); // Decrease balance
            } else { // Assuming if not expense, it's income
                List<SavingGoal> goals = savingGoalRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getTransactionDate(),
                        transaction.getTransactionDate()
                );
                if (!goals.isEmpty()) {
                    savingGoalRepository.updateSavedAmount(
                            transaction.getCategory(),
                            transaction.getWallet(),
                            transaction.getAmount(),
                            transaction.getTransactionDate()
                    );
                }
                newBalanceChange = transaction.getAmount(); // Increase balance
            }

            // Update Wallet Balance
            walletRepository.updateBalance(transaction.getWallet().getWalletId(), newBalanceChange);
        }

        return applicationMapper.toTransactionDTO(updatedTransaction);
    }

    @Override
    @Transactional
    public UUID deleteTransactionById(UUID transactionId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to delete this transaction");
        }

        // Reverse effects on Budget or SavingGoal
        if (TRANSACTION_TYPE_EXPENSE.equalsIgnoreCase(transaction.getType())) {
            List<Budget> budgets = budgetRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    transaction.getCategory(),
                    transaction.getWallet(),
                    transaction.getTransactionDate(),
                    transaction.getTransactionDate()
            );
            if (!budgets.isEmpty()) {
                budgetRepository.updateCurrentSpending(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getAmount().negate(),
                        transaction.getTransactionDate()
                );
            }
        } else if (TRANSACTION_TYPE_INCOME.equalsIgnoreCase(transaction.getType())) {
            List<SavingGoal> goals = savingGoalRepository.findByCategoryAndWalletAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    transaction.getCategory(),
                    transaction.getWallet(),
                    transaction.getTransactionDate(),
                    transaction.getTransactionDate()
            );
            if (!goals.isEmpty()) {
                savingGoalRepository.updateSavedAmount(
                        transaction.getCategory(),
                        transaction.getWallet(),
                        transaction.getAmount().negate(),
                        transaction.getTransactionDate()
                );
                // Refresh saving goals to update savedPercentage
                savingGoalService.getSavingGoalProgressAndAlerts();
            }
        }

        // Reverse Wallet Balance
        BigDecimal balanceChange = TRANSACTION_TYPE_INCOME.equalsIgnoreCase(transaction.getType())
                ? transaction.getAmount().negate() // Undo income
                : transaction.getAmount(); // Undo expense
        walletRepository.updateBalance(transaction.getWallet().getWalletId(), balanceChange);

        // Delete transaction
        transactionRepository.delete(transaction);
        return transactionId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDetailDTO> getTransactionsByDateRange(GetTransactionsByDateRangeDTO filter) {
        log.info("Fetching transactions from {} to {}", filter.getStartDate(), filter.getEndDate());

        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        List<Transaction> allTransactions = transactionRepository.findAllByWalletUser(currentUser);

        LocalDateTime startDateTime =
                filter.getStartDate() != null ? filter.getStartDate().atStartOfDay() : null;
        LocalDateTime endDateTime =
                filter.getEndDate() != null ? filter.getEndDate().atTime(LocalTime.MAX) : null;

        List<Transaction> filteredTransactions = allTransactions
                .stream()
                .filter(t -> {
                            if(startDateTime != null && endDateTime != null)
                                return !t.getTransactionDate().isBefore(startDateTime) && !t.getTransactionDate().isAfter(endDateTime);
                            else if(startDateTime != null)
                                return !t.getTransactionDate().isBefore(startDateTime);
                            else if(endDateTime != null)
                                return !t.getTransactionDate().isAfter(endDateTime);
                            else
                                return true;
                        }
                )
                .filter(t -> filter.getType() == null ||
                        t.getType().equalsIgnoreCase(filter.getType()))
                .filter(t -> filter.getCategory() == null ||
                        t.getCategory().getName().equalsIgnoreCase(filter.getCategory()))
                .filter(t -> {
                    if (filter.getTimeRange() == null || !filter.getTimeRange().contains("-")) return true;
                    try {
                        String[] parts = filter.getTimeRange().split("-");
                        LocalTime start = LocalTime.parse(parts[0].trim());
                        LocalTime end = LocalTime.parse(parts[1].trim());
                        LocalTime txnTime = t.getTransactionDate().toLocalTime();
                        return !txnTime.isBefore(start) && !txnTime.isAfter(end);
                    } catch (Exception e) {
                        return true; // fallback if parsing fails
                    }
                })
                .filter(t -> {
                    if (filter.getDayOfWeek() == null) return true;
                    try {
                        DayOfWeek expectedDay = DayOfWeek.valueOf(filter.getDayOfWeek().toUpperCase());
                        return t.getTransactionDate().getDayOfWeek() == expectedDay;
                    } catch (Exception e) {
                        return true;
                    }
                })
                .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
                .toList();

        return filteredTransactions.stream()
                .map(applicationMapper::toTransactionDetailDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDetailDTO> searchTransactions(SearchTransactionsDTO filter) {
        log.info("Searching transactions with filters from {} to {}", filter.getStartDate(), filter.getEndDate());

        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        LocalDateTime startDateTime =
                filter.getStartDate() != null ? filter.getStartDate().atStartOfDay() : null;
        LocalDateTime endDateTime =
                filter.getEndDate() != null ? filter.getEndDate().atTime(LocalTime.MAX) : null;

        List<Transaction> transactions = transactionRepository
                .findAllByWalletUserAndCategoryUser(currentUser)
                .stream()
                .filter(t -> {
                        if(startDateTime != null && endDateTime != null)
                            return !t.getTransactionDate().isBefore(startDateTime) && !t.getTransactionDate().isAfter(endDateTime);
                        else if(startDateTime != null)
                            return !t.getTransactionDate().isBefore(startDateTime);
                        else if(endDateTime != null)
                            return !t.getTransactionDate().isAfter(endDateTime);
                        else
                            return true;
                    }
                )
                .filter(t -> filter.getType() == null ||
                        t.getType().equalsIgnoreCase(filter.getType()))
                .filter(t -> filter.getCategoryName() == null ||
                        t.getCategory().getName().equalsIgnoreCase(filter.getCategoryName()))
                .filter(t -> filter.getWalletName() == null ||
                        t.getWallet().getWalletName().equalsIgnoreCase(filter.getWalletName()))
                .filter(t -> {
                    String amountRange = filter.getAmountRange();
                    if (amountRange == null) return true;
                    try {
                        BigDecimal amount = t.getAmount().abs();
                        if (amountRange.endsWith("+")) {
                            BigDecimal min = new BigDecimal(amountRange.replace("+", "").trim());
                            return amount.compareTo(min) >= 0;
                        } else if (amountRange.contains("-")) {
                            String[] parts = amountRange.split("-");
                            BigDecimal min = new BigDecimal(parts[0].trim());
                            BigDecimal max = new BigDecimal(parts[1].trim());
                            return amount.compareTo(min) >= 0 && amount.compareTo(max) <= 0;
                        } else {
                            return true;
                        }
                    } catch (Exception e) {
                        return true;
                    }
                })
                .filter(t -> filter.getKeywords() == null ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(filter.getKeywords().toLowerCase())))
                .filter(t -> {
                    if (filter.getTimeRange() == null || !filter.getTimeRange().contains("-")) return true;
                    try {
                        String[] times = filter.getTimeRange().split("-");
                        LocalTime start = LocalTime.parse(times[0]);
                        LocalTime end = LocalTime.parse(times[1]);
                        LocalTime txnTime = t.getTransactionDate().toLocalTime();
                        return !txnTime.isBefore(start) && !txnTime.isAfter(end);
                    } catch (Exception e) {
                        return true;
                    }
                })
                .filter(t -> {
                    if (filter.getDayOfWeek() == null) return true;
                    try {
                        DayOfWeek expected = DayOfWeek.valueOf(filter.getDayOfWeek().toUpperCase());
                        return t.getTransactionDate().getDayOfWeek() == expected;
                    } catch (Exception e) {
                        return true;
                    }
                })
                .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
                .toList();

        return transactions.stream()
                .map(applicationMapper::toTransactionDetailDTO)
                .toList();
    }
}