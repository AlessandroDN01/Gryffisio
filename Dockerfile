# 1. Build stage: usa Maven ufficiale con Java 17
FROM maven:3.9.6-eclipse-temurin-26 AS build
WORKDIR /app

# Copia prima il pom.xml per sfruttare la cache delle dipendenze Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia il codice sorgente e compila il JAR
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Run stage: immagine leggera con solo il JRE Java 17
FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]