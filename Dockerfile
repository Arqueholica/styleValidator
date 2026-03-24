FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/docstyler-2.0.0-SNAPSHOT.jar app.jar

RUN mkdir -p /app/data /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
