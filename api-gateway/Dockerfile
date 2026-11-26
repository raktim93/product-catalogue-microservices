# Stage 8: the same Dockerfile in every service folder.
# Build the jar first, from product-catalogue: ./mvnw clean package -DskipTests

# 1) builder: split the fat jar into layers (libraries change rarely, your code often)
FROM eclipse-temurin:21-jre AS builder
WORKDIR /builder
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# 2) runtime: only this stage ends up in the image
FROM eclipse-temurin:21-jre
LABEL authors="Raktim"
RUN groupadd --system spring && useradd --system --gid spring spring
WORKDIR /application
# One COPY per layer, the one that changes least first, so a code change reuses the others from the cache
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
USER spring
# The heap gets 75% of the container's memory limit (compose.yaml sets the limit, CAT-71)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]