package JavaProject.MoneyWise.controllers;

import JavaProject.MoneyWise.models.dtos.transaction.*;
import JavaProject.MoneyWise.services.TransactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @GetMapping("/date-range")
    public ResponseEntity<?> getTransactionsByDateRange(
            @Valid @ModelAttribute @ParameterObject GetTransactionsByDateRangeDTO dto,
            BindingResult bindingResult
    ) {

        if (bindingResult.hasErrors()) {
            // Collect validation errors into a simple list or string
            String errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(errors);
        }

        var transactions = transactionService.getTransactionsByDateRange(dto);

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchTransactions(
            @Valid @ModelAttribute @ParameterObject SearchTransactionsDTO dto,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(errors);
        }

        List<TransactionDetailDTO> transactions = transactionService.searchTransactions(dto);
        return ResponseEntity.ok(transactions);
    }

}
