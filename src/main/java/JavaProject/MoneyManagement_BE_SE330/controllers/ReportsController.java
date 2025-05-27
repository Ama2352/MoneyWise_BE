package JavaProject.MoneyManagement_BE_SE330.controllers;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.report.ReportInfoDTO;
import JavaProject.MoneyManagement_BE_SE330.services.TransactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Reports")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports")
public class ReportsController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/generate")
    public ResponseEntity<ByteArrayResource> generateReport(@Valid @RequestBody ReportInfoDTO reportInfo) {
        try {
            if (reportInfo.getEndDate() != null &&
                    reportInfo.getStartDate().isAfter(reportInfo.getEndDate())) {
                throw new RuntimeException("Start date must not be after end date");
            }

            // Create data
            Object reportData = transactionService.generateReportData(reportInfo);

            String reportType = reportInfo.getType().toLowerCase().replace("_", "-");
            String jasperPath = "src/main/resources/reports/" + reportType + ".jasper";
            File jasperFile = new File(jasperPath);
            if (!jasperFile.exists()) {
                throw new RuntimeException("Jasper file not found: " + jasperPath);
            }

            // Parameters for report
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("startDate", reportInfo.getStartDate());
            parameters.put("endDate", reportInfo.getEndDate());

            String currencySymbol = "VND";
            if ("USD".equalsIgnoreCase(reportInfo.getCurrency())) {
                currencySymbol = "USD";
            }
            parameters.put("currencySymbol", currencySymbol);

            JasperPrint jasperPrint;

            if ("cash-flow".equals(reportType)) {
                parameters.put("cashFlowData", reportData);
                jasperPrint = JasperFillManager.fillReport(jasperFile.getPath(), parameters, new JREmptyDataSource());
            } else {
                // Use JRBeanCollectionDataSource for other reports
                JRBeanCollectionDataSource dataSource;
                if (reportData instanceof List) {
                    dataSource = new JRBeanCollectionDataSource((List<?>) reportData);
                } else {
                    dataSource = new JRBeanCollectionDataSource(List.of(reportData));
                }
                jasperPrint = JasperFillManager.fillReport(jasperFile.getPath(), parameters, dataSource);
            }

            // Export PDF
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=report_" + reportType + "_" + System.currentTimeMillis() + ".pdf");
            headers.add("Content-Type", MediaType.APPLICATION_PDF_VALUE);
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate report data", e);
        }
    }
}
