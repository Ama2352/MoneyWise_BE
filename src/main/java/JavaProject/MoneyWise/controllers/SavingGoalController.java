package JavaProject.MoneyWise.controllers;

import JavaProject.MoneyWise.models.dtos.savingGoal.*;
import JavaProject.MoneyWise.services.SavingGoalService;
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
@RequestMapping("/api/SavingGoals")
@Tag(name = "Saving Goals")
public class SavingGoalController {
    private final SavingGoalService savingGoalService;

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new saving goal")
    @PostMapping
    public ResponseEntity<SavingGoalDTO> createSavingGoal(@RequestBody @Valid CreateSavingGoalDTO dto) {
        SavingGoalDTO createdSavingGoal = savingGoalService.createSavingGoal(dto);
        return ResponseEntity.ok(createdSavingGoal);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Retrieve all saving goals for the authenticated user")
    @GetMapping
    public ResponseEntity<List<SavingGoalDTO>> getAllSavingGoals() {
        List<SavingGoalDTO> savingGoals = savingGoalService.getAllSavingGoals();
        return ResponseEntity.ok(savingGoals);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Retrieve a specific saving goal by ID")
    @GetMapping("/{savingGoalId}")
    public ResponseEntity<SavingGoalDTO> getSavingGoalById(@PathVariable("savingGoalId") UUID savingGoalId) {
        SavingGoalDTO savingGoal = savingGoalService.getSavingGoalById(savingGoalId);
        return ResponseEntity.ok(savingGoal);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update an existing saving goal")
    @PutMapping("/{savingGoalId}")
    public ResponseEntity<SavingGoalDTO> updateSavingGoal(@RequestBody @Valid UpdateSavingGoalDTO dto) {
        SavingGoalDTO updatedSavingGoal = savingGoalService.updateSavingGoal(dto);
        return ResponseEntity.ok(updatedSavingGoal);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a saving goal by ID")
    @DeleteMapping("/{savingGoalId}")
    public ResponseEntity<UUID> deleteSavingGoal(@PathVariable("savingGoalId") UUID savingGoalId) {
        UUID deletedId = savingGoalService.deleteSavingGoal(savingGoalId);
        return ResponseEntity.ok(deletedId);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Search for savingGoals based on criteria")
    @GetMapping("/search")
    public ResponseEntity<List<SavingGoalProgressDTO>> searchSavingGoals(
            @Valid @ModelAttribute @ParameterObject SearchSavingGoalsDTO dto,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        List<SavingGoalProgressDTO> result = savingGoalService.searchSavingGoals(dto, acceptLanguage);
        return ResponseEntity.ok(result);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Retrieve progress and alerts for active saving goals")
    @GetMapping("/progress")
    public ResponseEntity<List<SavingGoalProgressDTO>> getSavingGoalProgressAndAlerts(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        List<SavingGoalProgressDTO> progress = savingGoalService.getSavingGoalProgressAndAlerts(acceptLanguage);
        return ResponseEntity.ok(progress);
    }
}