package JavaProject.MoneyWise.controllers;

import JavaProject.MoneyWise.helper.CurrencyConverter;
import JavaProject.MoneyWise.models.dtos.report.ReportInfoDTO;
import JavaProject.MoneyWise.models.dtos.statistic.CashFlowSummaryDTO;
import JavaProject.MoneyWise.services.StatisticService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/Reports")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports")
public class ReportsController {

    @Autowired
    private StatisticService statisticService;

    @Autowired
    private CurrencyConverter currencyConverter;

    private static final Logger logger = LoggerFactory.getLogger(ReportsController.class);

    @PostMapping("/generate")
    public ResponseEntity<ByteArrayResource> generateReport(
            @Valid @RequestBody ReportInfoDTO reportInfo,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        try {
            if (reportInfo.getEndDate() != null) {
                if (reportInfo.getStartDate().isAfter(reportInfo.getEndDate()))
                    throw new IllegalArgumentException("Start date must not be after end date");
            }

            // Pass acceptLanguage to service for localized report data
            Object reportData = statisticService.generateReportData(reportInfo, acceptLanguage);

            String currency = reportInfo.getCurrency() != null ? reportInfo.getCurrency().toUpperCase() : "VND";
            if (!currency.equals("VND") && !currency.equals("USD")) {
                throw new IllegalArgumentException("Unsupported currency: " + currency);
            }
            if ("USD".equalsIgnoreCase(currency)) {
                BigDecimal exchangeRate = currencyConverter.fetchExchangeRate();
                reportData = currencyConverter.convertForUSDReport(reportData, exchangeRate, reportInfo.getType());
            }

            // Determine report path
            String languageCode = acceptLanguage != null ? acceptLanguage.split(",")[0].split("-")[0] : "en";
            String reportType = reportInfo.getType().toLowerCase();
            String jasperPath = "reports/" + reportType + ".jasper";
            ClassPathResource jasperResource = new ClassPathResource(jasperPath);

            if (!jasperResource.exists()) {
                throw new IllegalArgumentException("Jasper file not found: " + jasperPath);
            }

            // Parameters for report
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("startDate", reportInfo.getStartDate());
            parameters.put("endDate", reportInfo.getEndDate());
            parameters.put("currencySymbol", currency);
            parameters.put("languageCode", languageCode);
            parameters.put("currencyConverter", currencyConverter);

            JasperPrint jasperPrint;
            if ("cash-flow".equalsIgnoreCase(reportInfo.getType()) && reportData instanceof CashFlowSummaryDTO cashFlow) {
                parameters.put("cashFlowData", cashFlow);
                jasperPrint = JasperFillManager.fillReport(jasperResource.getInputStream(), parameters, new JREmptyDataSource());
            } else {
                JRBeanCollectionDataSource dataSource;
                if (reportData instanceof List) {
                    dataSource = new JRBeanCollectionDataSource((List<?>) reportData);
                } else {
                    dataSource = new JRBeanCollectionDataSource(List.of(reportData));
                }
                jasperPrint = JasperFillManager.fillReport(jasperResource.getInputStream(), parameters, dataSource);
            }

            // Export PDF
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=report_" + reportType + "_" + languageCode + "_" + System.currentTimeMillis() + ".pdf");
            headers.add("Content-Type", MediaType.APPLICATION_PDF_VALUE);
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (JRException e) {
            logger.error("JasperReports error for report type {}: {}", reportInfo.getType(), e.getMessage(), e);
            throw new RuntimeException("Error generating report PDF", e);
        } catch (IOException e) {
            logger.error("IO error accessing Jasper file for report type {}: {}", reportInfo.getType(), e.getMessage(), e);
            throw new RuntimeException("Error accessing report template", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input for report type {}: {}", reportInfo.getType(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error generating report for type {}: {}", reportInfo.getType(), e.getMessage(), e);
            throw new RuntimeException("Internal server error while generating report", e);
        }
    }
}