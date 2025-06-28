package JavaProject.MoneyWise;

import net.sf.jasperreports.engine.JasperCompileManager;

public class JasperCompile {
    public static void main(String[] args) throws Exception {
        String source = "src/main/resources/reports/category-breakdown.jrxml";
        String dest = "src/main/resources/reports/category-breakdown.jasper";
        JasperCompileManager.compileReportToFile(source, dest);
        System.out.println("Compiled successfully!");
    }
}