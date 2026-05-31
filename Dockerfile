FROM maven:3.9.16-eclipse-temurin-25-alpine AS build

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

FROM container-registry.oracle.com/graalvm/jdk:25

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]