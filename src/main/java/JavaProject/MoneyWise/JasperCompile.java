package JavaProject.MoneyWise;

import net.sf.jasperreports.engine.JasperCompileManager;

import java.nio.file.Files;
import java.nio.file.Path;

public class JasperCompile {
    public static void main(String[] args) throws Exception {
        String reportsDir = "src/main/resources/reports";
        Files.walk(Path.of(reportsDir))
                .filter(path -> path.toString().endsWith(".jrxml"))
                .forEach(path -> {
                    try {
                        String dest = path.toString().replace(".jrxml", ".jasper");
                        JasperCompileManager.compileReportToFile(path.toString(), dest);
                        System.out.println("Compiled: " + path.getFileName());
                    } catch (Exception e) {
                        System.err.println("Failed to compile: " + path.getFileName());
                        e.printStackTrace();
                    }
                });
        System.out.println("All reports compiled!");
    }
}