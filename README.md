# Spring Boot course project template

Spring Boot 4 · Java 21 · Thymeleaf · JdbcTemplate · MySQL · H2 (tests) · GitHub Actions · Qodana · Azure App Service

## Using this template
1. **Use this template** → create the repo in the group's org
2. Rename: package `dk.ek.smartprojectplanner` (Shift+F6), `TemplateApplication` + its test, `artifactId`/`name` in `pom.xml`, `spring.application.name`, the database name `appdb` in `application-dev.properties`
3. Replace this README with the project's own
4. Then: Qodana token, Azure Web App + Deployment Center, env vars, ruleset

## Installation
Requirements: JDK 21, MySQL running locally.

1. `setx DEV_PASSWORD "yourpassword"` once, then restart IntelliJ
2. Run the application → http://localhost:8080 should say "Database: OK"

The database is created automatically. Tests need no database: `./mvnw clean test`