# Stage 1: Build con Maven y JDK 21 garantizados (evita que apt instale Java 25)
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar archivos del proyecto
COPY pom.xml .
COPY src ./src

# Compilar JAR omitiendo tests (los tests de integración requieren BD)
RUN mvn clean package -DskipTests

# Stage 2: Runtime con JRE 21 LTS
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiar JAR compilado
COPY --from=build /app/target/*.jar app.jar

# Crear directorio para subida de imágenes
RUN mkdir -p uploads

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
