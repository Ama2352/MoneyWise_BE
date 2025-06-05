package JavaProject.MoneyManagement_BE_SE330.controllers;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.statistic.CashFlowSummaryDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.statistic.CategoryBreakdownDTO;
import JavaProject.MoneyManagement_BE_SE330.services.StatisticService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/Statistics")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Statistics")
public class StatisticsController {
    private final StatisticService statisticService;

    @GetMapping("/category-breakdown")
    public ResponseEntity<List<CategoryBreakdownDTO>> getCategoryBreakdown(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<CategoryBreakdownDTO> breakdown = statisticService.getCategoryBreakdown(startDate, endDate);
        return ResponseEntity.ok(breakdown);
    }

    @GetMapping("/cash-flow")
    public ResponseEntity<CashFlowSummaryDTO> getCashFlowSummary(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        CashFlowSummaryDTO dto = statisticService.getCashFlowSummary(startDate, endDate);
        return ResponseEntity.ok(dto);
    }
}
