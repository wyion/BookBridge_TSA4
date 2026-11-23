# BookBridge_TSA4

Final BookBridge milestone: Java 17 Swing desktop GUI, JDBC SQLite persistence, DAO/service layering, and SwingWorker background operations. This project is a continuation of the provided TSA3 model and member policies, with additional database-backed behavior.

## Requirements
- JDK 17 or newer, Maven, graphical desktop (not the standard Programmiz console).
- Internet on first Maven build to fetch `org.xerial:sqlite-jdbc:3.46.1.3`.

## Run
1. Open the `BookBridge_TSA4` folder as a Maven project in NetBeans or another Java IDE.
2. Keep `schema.sql` in the working directory (project root).
3. Run `mvn compile exec:java` from the project root, or configure the IDE main class `bookbridge.app.Main` and project-root working directory.
4. A local `bookbridge.db` file is created automatically. The schema is initialized at startup. It is excluded from GitHub.
5. Optional seed: execute `seed_data.sql` **once** on an empty database using a SQLite client; do not repeat it on a populated DB.
6. Restart the application to confirm that data persists.

## Original TSA3 policies retained
Student: 14 days / PHP 5 per late day; Faculty: 30 days / PHP 2; Guest: 7 days / PHP 10. The TSA3 demonstration assumed concurrent active loan limits Student=2, Faculty=5, Guest=1. These are project assumptions rather than explicit limits in the assessment.

## GUI flow
Catalog: add/search/refresh; Members: register/refresh; Circulation: issue/return/refresh; Reservations: reserve/cancel/process next/refresh; Reports: load summary.

## Important limitations and testing notes
- This is a proposed TSA4 implementation and **has not been validated against a running SQLite JDBC driver or a graphical display in this environment**. Compile and integration testing are required before final submission.
- SQLite driver must be downloaded through Maven; standard Programmiz cannot demonstrate the GUI/JDBC integration.
- The app uses a `SwingWorker` for database calls. Refresh requests are asynchronous and can finish in a different order. The button enable/disable mechanism is basic and should be tested under repeated refresh.
- Database operations are transactional. SQLite foreign keys and partial unique indexes enforce important constraints.
- The shared-state hazard is concurrent reservation processing: two requests could both see the same available book. SQLite write transactions and the active-loan unique index prevent two committed active loans for the same ISBN; concurrent lock contention may produce a database-busy error and should be tested.
- Loan IDs are UUIDs rather than TSA3's sequential L001 style to avoid collisions across restarts.
- The existing TSA3 record types are represented as database-mapped records; review differences with the instructor-approved milestone.
- See `docs/TSA4_Test_Report.md` for **planned** tests. Do not label them PASS until you execute them and capture evidence.
- AI assistance: implementation scaffolding and documentation were AI-assisted; student must review, test, and explain all submitted work.
