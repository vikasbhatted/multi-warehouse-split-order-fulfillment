# Build context = repository root (docker compose passes `context: .`) so that ../bpmn-models is available.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend-springboot/pom.xml backend-springboot/pom.xml
COPY bpmn-models bpmn-models
RUN cd backend-springboot && mvn -q -B dependency:go-offline
COPY backend-springboot/src backend-springboot/src
RUN cd backend-springboot && mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --create-home appuser && mkdir /data && chown appuser /data
COPY --from=build /build/backend-springboot/target/split-order-fulfillment-1.0.0.jar app.jar
USER appuser
ENV H2_PATH=/data/fulfillment
VOLUME /data
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
