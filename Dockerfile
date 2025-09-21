FROM eclipse-temurin:17-jdk-alpine

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

COPY /source/target/*.jar app.jar

EXPOSE 8583

ENTRYPOINT ["java","-jar","app.jar", "server"]