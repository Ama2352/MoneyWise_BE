package JavaProject.MoneyWise.controllers;

import JavaProject.MoneyWise.models.dtos.statistic.DailySummaryDTO;
import JavaProject.MoneyWise.models.dtos.statistic.MonthlySummaryDTO;
import JavaProject.MoneyWise.models.dtos.statistic.WeeklySummaryDTO;
import JavaProject.MoneyWise.models.dtos.statistic.YearlySummaryDTO;
import JavaProject.MoneyWise.services.StatisticService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/Calendar")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Calendar")
public class CalendarController {
    private final StatisticService statisticService;

    // Daily summary
    @GetMapping("/daily")
    public ResponseEntity<DailySummaryDTO> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailySummaryDTO summary = statisticService.getDailySummary(date);
        return ResponseEntity.ok(summary);
    }

    // Weekly summary
    @GetMapping("/weekly")
    public ResponseEntity<WeeklySummaryDTO> getWeeklySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        WeeklySummaryDTO summary = statisticService.getWeeklySummary(startDate);
        return ResponseEntity.ok(summary);
    }

    // Monthly summary
    @GetMapping("/monthly")
    public ResponseEntity<MonthlySummaryDTO> getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        MonthlySummaryDTO summary = statisticService.getMonthlySummary(yearMonth);
        return ResponseEntity.ok(summary);
    }

    // Yearly summary
    @GetMapping("/yearly")
    public ResponseEntity<YearlySummaryDTO> getYearlySummary(@RequestParam int year) {
        YearlySummaryDTO summary = statisticService.getYearlySummary(year);
        return ResponseEntity.ok(summary);
    }
}
