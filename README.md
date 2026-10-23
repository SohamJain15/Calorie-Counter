# Calorie Counter

Log food consumption and calculate daily calories.
Plain HTML/CSS/JS frontend + Spring Boot backend + SQLite database.

## Pages
- **Overview** – today's calories vs goal, macro bars, today's food log, this week's calorie chart, and **smart meal suggestions** that fit your remaining calories and protein
- **Food log** – meals for any day (edit / delete entries), day balance (Calorie Surplus / Deficit), exercise
- **Progress** – 7 / 30 / 90-day chart, averages, days within goal, insights, and a **7-day calorie forecast** (linear regression + weekday pattern)
- **Food database** – searchable Indian-friendly foods with calories and macros per serving
- **Profile & goals** – personal details, calorie and macro goals (with a suggested goal calculator)
- **Settings** – light/dark theme, load a sample week, clear data

## Folder structure
```
calorie-counter-ip/
├── backend/                         Spring Boot app (Java 17+)
│   ├── mvnw.cmd                     Maven wrapper — no Maven install needed
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/caloriecounter/
│       │   ├── CalorieCounterApplication.java
│       │   ├── config/        WebConfig (CORS), DataSeeder (food database)
│       │   ├── controller/    Food, Log, Stats, Profile REST controllers
│       │   ├── dto/           request / response records (with validation)
│       │   ├── exception/     error handling
│       │   ├── model/         Food, FoodLogEntry, ExerciseEntry, UserProfile
│       │   ├── repository/    Spring Data JPA repositories
│       │   └── service/       business logic and calculations
│       ├── main/resources/application.properties
│       └── test/java/com/caloriecounter/CalorieApiTest.java
├── frontend/
│   ├── index.html                   app shell (sidebar + add-food dialog)
│   ├── app.js                       pages, routing, API calls
│   └── styles.css
├── .gitignore
└── README.md
```

## Run (Windows, from the project root)
```
cd backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```
Open **http://localhost:8081**. Spring Boot serves the frontend from `../frontend`, so nothing else needs to run.
Stop the server with `Ctrl + C`.

On first run the database file `backend/calorie_counter.db` is created and the food database is filled.
If the log is empty, 30 days of example meals and workouts are added automatically so every page has data.

## Build a runnable jar
```
cd backend
.\mvnw.cmd clean package
java -jar target\calorie-backend-1.0.0.jar
```
Run the jar from inside `backend` so it can find `../frontend`.

## API
See `backend/README.md`.
