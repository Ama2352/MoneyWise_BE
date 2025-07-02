# MoneyWise_BE

## Yêu cầu hệ thống

- Java 21 trở lên
- Maven 3.8+
- IntelliJ IDEA (khuyên dùng)
- PostgreSQL (nếu sử dụng database này)
- File cấu hình `.env` (đặt ở thư mục gốc project)

---

## 1. Clone & mở project

1. Clone project về máy:
    ```bash
    git clone https://github.com/your-username/MoneyWise_BE.git
    ```
2. Mở thư mục project bằng IntelliJ IDEA.

---

## 2. Cấu hình file `.env`

- Tạo file `.env` ở thư mục gốc (nếu chưa có).
- Thêm các biến môi trường cần thiết, ví dụ:
    ```
    DB_URL=jdbc:postgresql://localhost:5432/your_db
    DB_USERNAME=your_username
    DB_PASSWORD=your_password
    JWT_SECRET=your_jwt_secret
    # ... các biến khác nếu có
    ```

---

## 3. Cài đặt dependencies

- Trong IntelliJ, nhấn chuột phải vào file `pom.xml` → chọn **Reload Maven Project**.
- Hoặc chạy lệnh:
    ```bash
    mvn clean install
    ```

---

## 4. Compile các file JasperReports (.jrxml → .jasper)

- Đảm bảo đã build project (`mvn compile`).
- Chạy file `JasperCompile.java`:
    - Mở file `src/main/java/JavaProject/MoneyWise/JasperCompile.java` trong IntelliJ.
    - Nhấn chuột phải vào file → chọn **Run 'JasperCompile.main()'**.
    - Kết quả: Tất cả file `.jrxml` trong `src/main/resources/reports` sẽ được compile thành `.jasper`.

---

## 5. Chạy ứng dụng

- Chạy class main của Spring Boot:  
    Mở file `MoneyWiseApplication.java` (hoặc tên tương tự trong package `JavaProject.MoneyWise`)  
    Nhấn chuột phải → **Run 'MoneyWiseApplication.main()'**.

- Hoặc chạy bằng terminal:
    ```bash
    mvn spring-boot:run
    ```

---

## 6. Kiểm tra API

- Mở trình duyệt và truy cập:  
  ```
  http://localhost:8080/swagger-ui.html
  ```
  (hoặc đường dẫn swagger khác nếu bạn cấu hình)

---

## 7. Một số lưu ý

- Nếu thay đổi file `.jrxml`, hãy chạy lại `JasperCompile.java` để cập nhật file `.jasper`.
- Đảm bảo file `.env` đúng định dạng và đủ biến môi trường.
- Nếu gặp lỗi liên quan đến JasperReports, kiểm tra lại file `.jasper` đã được compile mới nhất chưa.

---

## 8. Tham khảo

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [JasperReports Documentation](https://community.jaspersoft.com/documentation)

---

**Chúc bạn
