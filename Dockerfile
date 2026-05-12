# -- Stage 1: Build React frontend --
FROM node:22-alpine AS frontend
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --prefer-offline
COPY frontend/ ./
RUN npm run build

# -- Stage 2: Build Spring Boot JAR --
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q 2>/dev/null || true
COPY src/ src/
COPY --from=frontend /workspace/src/main/resources/static src/main/resources/static
RUN ./gradlew bootJar -x installFrontend -x buildFrontend --no-daemon -q

# -- Stage 3: Lightweight runtime --
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S bil && adduser -S bil -G bil
RUN mkdir -p data && chown bil:bil data
COPY --from=builder /workspace/build/libs/bil-*.jar app.jar
RUN chown bil:bil app.jar
USER bil
EXPOSE ${PORT:-8080}
CMD ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8080} -jar app.jar"]
