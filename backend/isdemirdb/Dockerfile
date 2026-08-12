# Build stage. pom.xml is copied alone first so the dependency download lands in its own
# layer - editing Java code then rebuilds in seconds instead of re-pulling all of Maven.
# The --mount=type=cache keeps the Maven repo OUTSIDE the layers as well: that layer cache
# is dropped on every pom.xml edit, and without this a single new dependency means
# re-downloading all of Maven (~10 minutes).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
