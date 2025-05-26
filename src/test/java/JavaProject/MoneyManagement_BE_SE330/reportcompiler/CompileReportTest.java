package JavaProject.MoneyManagement_BE_SE330.reportcompiler;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JRException;

import java.util.Arrays;
import java.util.List;

public class CompileReportTest {

    public static void main(String[] args) throws JRException {
        List<String> reportFiles = Arrays.asList(
                "cash-flow.jrxml",
                "category-breakdown.jrxml",
                "daily-summary.jrxml",
                "monthly-detail-subreport.jrxml",
                "weekly-detail-subreport.jrxml",
                "yearly-detail-subreport.jrxml",
                "monthly-summary.jrxml",
                "weekly-summary.jrxml",
                "yearly-summary.jrxml"
        );

        String inputDir = "src/main/resources/reports/";
        String outputDir = "src/main/resources/reports/";

        for (String fileName : reportFiles) {
            String inputPath = inputDir + fileName;
            String outputPath = outputDir + fileName.replace(".jrxml", ".jasper");

            JasperCompileManager.compileReportToFile(inputPath, outputPath);
            System.out.println("Compiled: " + fileName + " → " + outputPath);
        }

        System.out.println("Tất cả báo cáo đã được biên dịch xong.");
    }
}
