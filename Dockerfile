# === Stage 1: Build the app with Maven ===
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# === Stage 2: Run the app ===
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/MoneyWise-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# Use CMD with sh -c to echo environment variables and run the app
CMD ["sh", "-c", "echo DB_URL=$DB_URL && echo DB_USERNAME=$DB_USERNAME && echo DB_PASSWORD=$DB_PASSWORD && java -jar app.jar"]