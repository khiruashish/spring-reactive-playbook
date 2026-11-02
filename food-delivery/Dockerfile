# Multi-stage build for any service:  docker build --build-arg SERVICE=order-service -f food-delivery/Dockerfile ..
FROM eclipse-temurin:21-jdk AS build
ARG SERVICE
WORKDIR /src
COPY . .
RUN ./gradlew :food-delivery:${SERVICE}:bootJar --no-daemon -q \
 && cp food-delivery/${SERVICE}/build/libs/${SERVICE}-*.jar /app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
USER app
COPY --from=build /app.jar /app.jar
# Container-aware heap; Netty event loops size themselves from available CPUs
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app.jar"]
