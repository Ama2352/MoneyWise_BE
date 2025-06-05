package JavaProject.MoneyManagement_BE_SE330.services;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.report.ReportInfoDTO;
import JavaProject.MoneyManagement_BE_SE330.models.dtos.statistic.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface StatisticService {
    List<CategoryBreakdownDTO> getCategoryBreakdown(LocalDate startDate, LocalDate endDate);
    DailySummaryDTO getDailySummary(LocalDate date);
    WeeklySummaryDTO getWeeklySummary(LocalDate weekStartDate);
    MonthlySummaryDTO getMonthlySummary(YearMonth yearMonth);
    YearlySummaryDTO getYearlySummary(int year);
    CashFlowSummaryDTO getCashFlowSummary(LocalDate startDate, LocalDate endDate);
    Object generateReportData(ReportInfoDTO reportInfo);
}
