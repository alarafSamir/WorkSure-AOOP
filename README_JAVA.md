# WorkSure Java backend

The complete setup and project explanation are in [README.md](README.md).

The application uses Java 17+, Spring Boot, Spring Security/JWT, Spring JDBC, MariaDB and the included Gradle wrapper. Its frontend is plain HTML, CSS and JavaScript served by Spring Boot.

## Run on Fedora/Linux

```bash
sudo systemctl start mariadb
cd /home/samir/Documents/worksure/backend
./gradlew bootRun
```

Open http://localhost:5000. Configure `spring.datasource.*` in `src/main/resources/application.properties` if your database credentials differ. On an empty database the application initializes tables and demo data. Do not overwrite an existing database with setup SQL.

## Build

```bash
./gradlew clean build
java -jar build/libs/worksure-backend-1.0.0.jar
```

Use either `bootRun` or the JAR, not both at once. Run from `backend/` to keep upload paths consistent. Back up MariaDB, `uploads/`, and `private-documents/` together. The preserved optional real-time backend occupies port 9092; the website only needs port 5000.

Demo admin: `admin@gmail.com` / `12345`. Demo worker: `rahim@gmail.com` / `12345`. Register a customer through the website.
