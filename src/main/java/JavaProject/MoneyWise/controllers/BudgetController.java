package JavaProject.MoneyWise.controllers;

import JavaProject.MoneyWise.models.dtos.budget.*;
import JavaProject.MoneyWise.services.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/Budgets")
@Tag(name = "Budgets")
public class BudgetController {
    private final BudgetService budgetService;

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new budget")
    @PostMapping
    public ResponseEntity<BudgetDTO> createBudget(@RequestBody @Valid CreateBudgetDTO dto) {
        BudgetDTO createdBudget = budgetService.createBudget(dto);
        return ResponseEntity.ok(createdBudget);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Retrieve all budgets for the authenticated user")
    @GetMapping
    public ResponseEntity<List<BudgetDTO>> getAllBudgets() {
        List<BudgetDTO> budgets = budgetService.getAllBudgets().stream()
                .map(budget -> budgetService.getBudgetById(budget.getBudgetId()))
                .toList();
        return ResponseEntity.ok(budgets);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Retrieve a specific budget by ID")
    @GetMapping("/{budgetId}")
    public ResponseEntity<BudgetDTO> getBudgetById(@PathVariable("budgetId") UUID budgetId) {
        BudgetDTO budget = budgetService.getBudgetById(budgetId);
        return ResponseEntity.ok(budget);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update an existing budget")
    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetDTO> updateBudget(@RequestBody @Valid UpdateBudgetDTO dto) {
        BudgetDTO updatedBudget = budgetService.updateBudget(dto);
        return ResponseEntity.ok(updatedBudget);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a budget by ID")
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<UUID> deleteBudget(@PathVariable("budgetId") UUID budgetId) {
        UUID deletedId = budgetService.deleteBudget(budgetId);
        return ResponseEntity.ok(deletedId);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Search for budgets based on criteria")
    @GetMapping("/search")
    public ResponseEntity<List<BudgetProgressDTO>> searchBudgets(
            @Valid @ModelAttribute @ParameterObject SearchBudgetsDTO dto,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        List<BudgetProgressDTO> result = budgetService.searchBudgets(dto, acceptLanguage);
        return ResponseEntity.ok(result);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Retrieve progress and alerts for active budgets")
    @GetMapping("/progress")
    public ResponseEntity<List<BudgetProgressDTO>> getBudgetProgressAndAlerts(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "currency", required = false) String currency) {
        List<BudgetProgressDTO> progress = budgetService.getBudgetProgressAndAlerts(acceptLanguage, currency);
        return ResponseEntity.ok(progress);
    }
}