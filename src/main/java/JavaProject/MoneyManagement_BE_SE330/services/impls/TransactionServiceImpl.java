package JavaProject.MoneyManagement_BE_SE330.services.impls;

import JavaProject.MoneyManagement_BE_SE330.helper.ApplicationMapper;
import JavaProject.MoneyManagement_BE_SE330.helper.HelperFunctions;
import JavaProject.MoneyManagement_BE_SE330.helper.ResourceNotFoundException;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.CreateTransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.TransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.UpdateTransactionDTO;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Category;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Transaction;
import JavaProject.MoneyManagement_BE_SE330.models.entities.User;
import JavaProject.MoneyManagement_BE_SE330.models.entities.Wallet;
import JavaProject.MoneyManagement_BE_SE330.repositories.CategoryRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.TransactionRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.UserRepository;
import JavaProject.MoneyManagement_BE_SE330.repositories.WalletRepository;
import JavaProject.MoneyManagement_BE_SE330.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final ApplicationMapper applicationMapper;
    private final UserRepository userRepository;

    @Override
    public List<TransactionDTO> getAllTransactions() {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        return transactionRepository.findAllByWalletUser(currentUser)
                .stream()
                .map(applicationMapper::toTransactionDTO)
                .toList();
    }

    @Override
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
    public TransactionDTO createTransaction(CreateTransactionDTO model) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);

        Wallet wallet = walletRepository.findById(model.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if(!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this wallet");
        }

        Category category = categoryRepository.findById(model.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if(!category.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not own this category");
        }

        Transaction transaction = applicationMapper.toTransactionEntity(model);
        transaction.setWallet(wallet);
        transaction.setCategory(category);

        transactionRepository.save(transaction);
        return applicationMapper.toTransactionDTO(transaction);
    }

    @Override
    public TransactionDTO updateTransaction(UpdateTransactionDTO model) {
        var transaction = transactionRepository.findById(model.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Update fields - assuming UpdateTransactionDTO has the fields
        transaction.setAmount(model.getAmount());
        transaction.setDescription(model.getDescription());
        transaction.setTransactionDate(model.getTransactionDate());
        transaction.setType(model.getType());

        // Fetch Wallet entity by ID
        Wallet wallet = walletRepository.findById(model.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        // Fetch Category entity by ID
        Category category = categoryRepository.findById(model.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        User currentUser = HelperFunctions.getCurrentUser(userRepository);

        if(!transaction.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to update this transaction");
        }

        if(!wallet.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You cannot move transaction to a wallet you don't own");
        }

        if (!category.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You cannot assign a category you don't own");
        }

        // Set the references on your Transaction entity
        transaction.setWallet(wallet);
        transaction.setCategory(category);

        transactionRepository.save(transaction);
        return applicationMapper.toTransactionDTO(transaction);
    }

    @Override
    public UUID deleteTransactionById(UUID transactionId) {
        User currentUser = HelperFunctions.getCurrentUser(userRepository);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if(!transaction.getWallet().getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have access to delete this transaction");
        }

        transactionRepository.delete(transaction);
        return transactionId;
    }
}
