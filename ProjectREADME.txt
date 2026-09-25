WorkSure — final AOOP project

Stack: HTML, CSS, JavaScript, Java, Spring Boot, MariaDB, JDBC, Gradle.

Run on Fedora/Linux:
1. sudo systemctl start mariadb
2. If needed, configure spring.datasource settings in backend/src/main/resources/application.properties.
3. cd /home/samir/Documents/worksure/backend
4. ./gradlew bootRun
5. Open http://localhost:5000

One Spring Boot server serves both the website and API. No frontend installation or separate frontend server is needed.

Build: from backend/, run ./gradlew clean build
Run the built JAR: java -jar build/libs/worksure-backend-1.0.0.jar
Stop the existing server before starting another instance.

Admin demo: admin@gmail.com / 12345
Worker demo: rahim@gmail.com / 12345
Register a customer in the browser.

Read README.md for database setup, structure, OOP explanation and limitations.
Read docs/final-migration-report.md for final test results and file changes.
