# Calorie Counter — Spring Boot + SQLite backend

## Stack
- Java 17+, Spring Boot 3.3 (Web, Data JPA, Validation)
- SQLite via `sqlite-jdbc` + Hibernate `SQLiteDialect`
- DB file: `calorie_counter.db`, created in the folder you run the app from; tables are created automatically

## Run (Windows)
```
cd backend
.\mvnw.cmd spring-boot:run
```
`mvnw.cmd` downloads Maven automatically on first run. Then open **http://localhost:8081/**.

## Tables
| Table | What it stores |
|---|---|
| `foods` | Food database: name, category, serving size, calories, protein, carbs, fat (per serving) |
| `food_log_entries` | What was eaten: date, meal, time, food, servings, total calories and macros |
| `exercise_entries` | Exercise: date, time, name, minutes, calories burned |
| `user_profile` | Name, age, sex, height, weight, activity level, calorie and macro goals |

## API
All request/response bodies are JSON. Meals: `breakfast`, `lunch`, `dinner`, `snack`.
Categories: `indian`, `fruits`, `vegetables`, `grains`, `dairy`, `meat`, `other`.

| Method | Path | What it does |
|---|---|---|
| GET | `/api/foods?q=&category=` | Search the food database |
| GET | `/api/foods/{id}` | One food |
| POST | `/api/foods` | Add a custom food |
| DELETE | `/api/foods/{id}` | Remove a food |
| GET | `/api/log?date=YYYY-MM-DD` | Foods and exercise logged on a day (default today) |
| POST | `/api/log/entries` | Log a food |
| PUT | `/api/log/entries/{id}` | Edit a logged food |
| DELETE | `/api/log/entries/{id}` | Delete a logged food |
| POST | `/api/log/exercises` | Log exercise |
| PUT | `/api/log/exercises/{id}` | Edit exercise |
| DELETE | `/api/log/exercises/{id}` | Delete exercise |
| DELETE | `/api/log/day/{date}` | Clear one day |
| DELETE | `/api/log/all` | Delete all food and exercise history |
| GET | `/api/summary?date=` | Day totals, macros, per-meal calories, Surplus/Deficit |
| GET | `/api/progress?days=7` | Daily history (1–365 days), averages, days within goal |
| GET | `/api/forecast?days=7` | Predicted calories for the next days from tomorrow (linear regression + weekday pattern) |
| GET | `/api/suggestions?date=` | Meal ideas that fit the remaining calories and protein |
| GET / PUT | `/api/profile` | Profile and goals |
| POST | `/api/sample-data?days=30` | Fill the last N days + today's past meals with example data |
| POST | `/api/sample-data/week` | Fill the previous 7 days with example meals |

### Log a food from the database
```json
POST /api/log/entries
{ "date": "2026-10-23", "mealType": "breakfast", "time": "08:30", "foodId": 6, "quantity": 1.5 }
```
Calories and macros are taken from the food and multiplied by `quantity`.

### Log a custom food
```json
POST /api/log/entries
{ "mealType": "snack", "name": "Homemade chivda", "servingSize": "1 small bowl",
  "calories": 150, "protein": 3, "carbs": 18, "fat": 7, "quantity": 1 }
```
`calories` and macros are per serving.

### Day summary
```json
GET /api/summary
{ "date": "2026-10-23", "goal": 2200, "consumed": 1420, "burned": 260, "remaining": 1040,
  "status": "Deficit", "percentConsumed": 65, "protein": 63.0, "carbs": 184.0, "fat": 45.5, ... }
```
`remaining = goal − consumed + burned`; `status` is `Surplus` when remaining is below 0, otherwise `Deficit`.

### Errors
Validation problems return `400`:
```json
{ "status": 400, "error": "Please check the highlighted values", "details": ["quantity must be at least 0.1"] }
```
Missing items return `404`.

### Forecast (how it works)
1. Take the logged days in the last 28 days (today is excluded because it isn't finished).
2. Fit a straight line `calories = a + b × day` by least squares.
3. For each weekday, average how far real days were from the line and add half of that back
   (captures habits like heavier weekends without over-reacting to one big day).
4. The range shown is the prediction ± one standard deviation of the remaining error. `rSquared` says how
   much of the day-to-day variation the model explains.

### Meal suggestions (how it works)
1. Next meal = first meal not yet logged today (never earlier than the time of day suggests).
2. Calorie target = calories left today, capped at a normal size for that meal (breakfast 500, lunch/dinner 700, snack 250).
3. Candidates are real meals built from foods that suit that meal: a curry/sabzi with roti, rice or bread,
   or a complete dish (biryani, dosa, poha…) on its own, optionally with a light side (curd, salad, chaas).
4. Keep candidates between 55% and 100% of the target and score them: 60% protein coverage + 40% calorie fit,
   minus a penalty when more than 40% of the calories come from fat. The top 3 with different main foods are returned.
5. If the day is already over budget, light options (≤150 kcal, highest protein per calorie) are suggested instead.

On first start, if the food log is empty, 30 days of example data are created
(turn off with `app.sample-data.on-empty=false` in `application.properties`).
