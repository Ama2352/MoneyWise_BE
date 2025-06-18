package JavaProject.MoneyWise.helper;

import JavaProject.MoneyWise.models.dtos.statistic.*;
import JavaProject.MoneyWise.models.dtos.statistic.CategoryBreakdownDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CurrencyConverter {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(CurrencyConverter.class);

    // Primary exchange rate API
    private static final String PRIMARY_CURRENCY_API = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json";

    // Fallback API if primary fails
    private static final String FALLBACK_CURRENCY_API = "https://latest.currency-api.pages.dev/v1/currencies/usd.json";

    // Fetch exchange rate USD to VND from API
    public BigDecimal fetchExchangeRate() {
        try {
            String response = restTemplate.getForObject(new URI(PRIMARY_CURRENCY_API), String.class);
            return parseExchangeRate(response);
        } catch (Exception e) {
            logger.warn("Primary currency API failed: {}", e.getMessage());
            try {
                String response = restTemplate.getForObject(new URI(FALLBACK_CURRENCY_API), String.class);
                return parseExchangeRate(response);
            } catch (Exception ex) {
                logger.error("Fallback currency API failed: {}", ex.getMessage());
                throw new RuntimeException("Unable to fetch exchange rate", ex);
            }
        }
    }

    // Parse exchange rate value from API response
    private BigDecimal parseExchangeRate(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode vndRateNode = jsonNode.path("usd").path("vnd");
            if (vndRateNode.isMissingNode()) {
                throw new RuntimeException("VND rate not found in response");
            }
            return vndRateNode.decimalValue().setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            logger.error("Parse error: {}", e.getMessage());
            throw new RuntimeException("Unable to parse exchange rate", e);
        }
    }

    // Convert report data from VND to USD
    public Object convertToUSD(Object reportData, BigDecimal exchangeRate, String reportType) {
        if (reportData == null) {
            throw new IllegalStateException("Report data is null: " + reportType);
        }

        switch (reportType.toLowerCase()) {
            case "category-breakdown":
                if (!(reportData instanceof List<?> list) || !list.stream().allMatch(item -> item instanceof CategoryBreakdownDTO)) {
                    throw new IllegalStateException("Expected List<CategoryBreakdownDTO>");
                }
                break;
            case "cash-flow":
                if (!(reportData instanceof CashFlowSummaryDTO)) {
                    throw new IllegalStateException("Expected CashFlowSummaryDTO");
                }
                break;
            case "daily-summary":
                if (!(reportData instanceof DailySummaryDTO)) {
                    throw new IllegalStateException("Expected DailySummaryDTO");
                }
                break;
            case "weekly-summary":
                if (!(reportData instanceof WeeklySummaryDTO)) {
                    throw new IllegalStateException("Expected WeeklySummaryDTO");
                }
                break;
            case "monthly-summary":
                if (!(reportData instanceof MonthlySummaryDTO)) {
                    throw new IllegalStateException("Expected MonthlySummaryDTO");
                }
                break;
            case "yearly-summary":
                if (!(reportData instanceof YearlySummaryDTO)) {
                    throw new IllegalStateException("Expected YearlySummaryDTO");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported report type: " + reportType);
        }

        try {
            // Convert list or single DTO
            if (reportData instanceof List) {
                return ((List<?>) reportData).stream()
                        .map(dto -> convertSingleDTOToUSD(dto, exchangeRate))
                        .collect(Collectors.toList());
            } else {
                return convertSingleDTOToUSD(reportData, exchangeRate);
            }
        } catch (Exception e) {
            logger.error("Conversion error for {}: {}", reportType, e.getMessage(), e);
            throw new RuntimeException("Conversion to USD failed", e);
        }
    }

    // Convert individual DTO to USD
    private Object convertSingleDTOToUSD(Object dto, BigDecimal exchangeRate) {
        if (dto == null) return null;

        if (dto instanceof CategoryBreakdownDTO categoryDTO) {
            return new CategoryBreakdownDTO(
                    categoryDTO.getCategory(),
                    convertBigDecimal(categoryDTO.getTotalIncome(), exchangeRate),
                    convertBigDecimal(categoryDTO.getTotalExpense(), exchangeRate),
                    categoryDTO.getIncomePercentage(),
                    categoryDTO.getExpensePercentage(),
                    convertBigDecimal(categoryDTO.getBudgetLimit(), exchangeRate),
                    convertBigDecimal(categoryDTO.getBudgetCurrentSpending(), exchangeRate),
                    convertBigDecimal(categoryDTO.getGoalTarget(), exchangeRate),
                    convertBigDecimal(categoryDTO.getGoalSaved(), exchangeRate)
            );
        } else if (dto instanceof CashFlowSummaryDTO cashFlowDTO) {
            return new CashFlowSummaryDTO(
                    convertBigDecimal(cashFlowDTO.getTotalIncome(), exchangeRate),
                    convertBigDecimal(cashFlowDTO.getTotalExpenses(), exchangeRate)
            );
        } else if (dto instanceof DailySummaryDTO dailyDTO) {
            List<DailyDetailDTO> convertedDetails = dailyDTO.getDailyDetails().stream()
                    .map(detail -> new DailyDetailDTO(
                            detail.getDayOfWeek(),
                            convertBigDecimal(detail.getIncome(), exchangeRate),
                            convertBigDecimal(detail.getExpense(), exchangeRate)
                    ))
                    .collect(Collectors.toList());
            DailySummaryDTO result = new DailySummaryDTO();
            result.setDailyDetails(convertedDetails);
            result.setTotalIncome(convertBigDecimal(dailyDTO.getTotalIncome(), exchangeRate));
            result.setTotalExpenses(convertBigDecimal(dailyDTO.getTotalExpenses(), exchangeRate));
            return result;
        } else if (dto instanceof WeeklySummaryDTO weeklyDTO) {
            List<WeeklyDetailDTO> convertedDetails = weeklyDTO.getWeeklyDetails().stream()
                    .map(detail -> {
                        WeeklyDetailDTO newDetail = new WeeklyDetailDTO();
                        newDetail.setWeekNumber(detail.getWeekNumber());
                        newDetail.setIncome(convertBigDecimal(detail.getIncome(), exchangeRate));
                        newDetail.setExpense(convertBigDecimal(detail.getExpense(), exchangeRate));
                        return newDetail;
                    })
                    .collect(Collectors.toList());
            WeeklySummaryDTO result = new WeeklySummaryDTO();
            result.setWeeklyDetails(convertedDetails);
            result.setTotalIncome(convertBigDecimal(weeklyDTO.getTotalIncome(), exchangeRate));
            result.setTotalExpenses(convertBigDecimal(weeklyDTO.getTotalExpenses(), exchangeRate));
            return result;
        } else if (dto instanceof MonthlySummaryDTO monthlyDTO) {
            List<MonthlyDetailDTO> convertedDetails = monthlyDTO.getMonthlyDetails().stream()
                    .map(detail -> {
                        MonthlyDetailDTO newDetail = new MonthlyDetailDTO();
                        newDetail.setMonthName(detail.getMonthName());
                        newDetail.setIncome(convertBigDecimal(detail.getIncome(), exchangeRate));
                        newDetail.setExpense(convertBigDecimal(detail.getExpense(), exchangeRate));
                        return newDetail;
                    })
                    .collect(Collectors.toList());
            MonthlySummaryDTO result = new MonthlySummaryDTO();
            result.setMonthlyDetails(convertedDetails);
            result.setTotalIncome(convertBigDecimal(monthlyDTO.getTotalIncome(), exchangeRate));
            result.setTotalExpenses(convertBigDecimal(monthlyDTO.getTotalExpenses(), exchangeRate));
            return result;
        } else if (dto instanceof YearlySummaryDTO yearlyDTO) {
            List<YearlyDetailDTO> convertedDetails = yearlyDTO.getYearlyDetails().stream()
                    .map(detail -> {
                        YearlyDetailDTO newDetail = new YearlyDetailDTO();
                        newDetail.setYear(detail.getYear());
                        newDetail.setIncome(convertBigDecimal(detail.getIncome(), exchangeRate));
                        newDetail.setExpense(convertBigDecimal(detail.getExpense(), exchangeRate));
                        return newDetail;
                    })
                    .collect(Collectors.toList());
            YearlySummaryDTO result = new YearlySummaryDTO();
            result.setYearlyDetails(convertedDetails);
            result.setTotalIncome(convertBigDecimal(yearlyDTO.getTotalIncome(), exchangeRate));
            result.setTotalExpenses(convertBigDecimal(yearlyDTO.getTotalExpenses(), exchangeRate));
            return result;
        }

        return dto;
    }

    // Divide VND value by exchange rate to get USD, round to 2 decimals
    private BigDecimal convertBigDecimal(BigDecimal value, BigDecimal exchangeRate) {
        if (value == null) return BigDecimal.ZERO;
        return value.divide(exchangeRate, 2, RoundingMode.HALF_UP);
    }
}