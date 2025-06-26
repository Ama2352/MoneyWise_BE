package JavaProject.MoneyWise;

import net.sf.jasperreports.engine.JasperCompileManager;

public class JasperCompile {
    public static void main(String[] args) throws Exception {
        String source = "src/main/resources/reports/cash-flow.jrxml";
        String dest = "src/main/resources/reports/cash-flow.jasper";
        JasperCompileManager.compileReportToFile(source, dest);
        System.out.println("Compiled successfully!");
    }
}