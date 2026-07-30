# ============================================================================
# TICKET-ADV146 — Spring Boot Dockerfile (multi-stage Maven -> JRE 25 slim)
# ============================================================================

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copy the Maven wrapper from the repo root
COPY mvnw .
COPY .mvn .mvn

# Fix execution permissions on the wrapper script
RUN chmod +x mvnw

# 1. Build the starter module and install it to the container's local cache
COPY recon-audit-starter ./recon-audit-starter
WORKDIR /workspace/recon-audit-starter
RUN ../mvnw -q -B clean install

# 2. Build the backend application
WORKDIR /workspace/backend
COPY backend/pom.xml .
RUN ../mvnw -q -B dependency:go-offline || true
COPY backend/src ./src
RUN ../mvnw -q -B -DskipTests package && \
    mv target/reconx-*.jar /workspace/app.jar

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S reconx && adduser -S reconx -G reconx
USER reconx
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]