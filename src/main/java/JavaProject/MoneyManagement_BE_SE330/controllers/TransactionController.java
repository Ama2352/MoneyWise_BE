package JavaProject.MoneyManagement_BE_SE330.controllers;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.transaction.*;
import JavaProject.MoneyManagement_BE_SE330.services.TransactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/Transactions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transaction")
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        List<TransactionDTO> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByWalletId(@PathVariable("walletId") UUID walletId) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByWalletId(walletId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable("transactionId") UUID transactionId) {
        TransactionDTO found = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(found);
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@RequestBody @Valid CreateTransactionDTO model) {
        TransactionDTO created = transactionService.createTransaction(model);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<TransactionDTO> updateTransaction(@RequestBody @Valid UpdateTransactionDTO model) {
        TransactionDTO updated = transactionService.updateTransaction(model);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<UUID> deleteTransaction(@PathVariable("transactionId") UUID transactionId) {
        UUID deletedId = transactionService.deleteTransactionById(transactionId);
        return ResponseEntity.ok(deletedId);
    }
}
