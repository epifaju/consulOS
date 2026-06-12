FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S consulos && adduser -S consulos -G consulos
COPY target/consulos-backend-*.jar app.jar
RUN chown consulos:consulos app.jar
USER consulos
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
