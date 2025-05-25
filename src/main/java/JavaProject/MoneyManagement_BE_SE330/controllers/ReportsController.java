package JavaProject.MoneyManagement_BE_SE330.controllers;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.report.ReportInfoDTO;
import JavaProject.MoneyManagement_BE_SE330.services.TransactionService;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class ReportsController {

    private final TransactionService transactionService;

    @PostMapping("/generate")
    public ResponseEntity<Resource> generateReport(@RequestBody ReportInfoDTO reportInfo) {
        try {
            String reportType = reportInfo.getType().replace("-", "_");
            String jasperPath = "src/main/resources/reports/" + reportType + ".jasper";

            Object reportData = transactionService.generateReportData(reportInfo);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("startDate", reportInfo.getStartDate());
            parameters.put("endDate", reportInfo.getEndDate());

            JRBeanCollectionDataSource dataSource;
            if (reportData instanceof List) {
                dataSource = new JRBeanCollectionDataSource((List<?>) reportData);
            } else {
                dataSource = new JRBeanCollectionDataSource(List.of(reportData));
            }

            var jasperPrint = JasperFillManager.fillReport(jasperPath, parameters, dataSource);
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            ByteArrayResource resource = new ByteArrayResource(pdfBytes);
            String fileName = reportType.replace("_", "-") + "_report.pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + fileName);
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(pdfBytes.length)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate report data", e);
        }
    }
}