package com.caloriecounter;

import com.caloriecounter.repository.ExerciseEntryRepository;
import com.caloriecounter.repository.FoodLogEntryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:sqlite:target/test-calorie.db", "app.sample-data.on-empty=false"})
@AutoConfigureMockMvc
class CalorieApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FoodLogEntryRepository entries;
    @Autowired ExerciseEntryRepository exercises;

    @BeforeEach
    void clean() {
        entries.deleteAll();
        exercises.deleteAll();
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long foodId(String name) throws Exception {
        String res = mvc.perform(get("/api/foods").param("q", name)).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get(0).get("id").asLong();
    }

    @Test
    void foodDatabaseIsSeededAndSearchable() throws Exception {
        mvc.perform(get("/api/foods")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(greaterThan(30))));
        mvc.perform(get("/api/foods").param("q", "paneer"))
                .andExpect(jsonPath("$[*].name", everyItem(containsStringIgnoringCase("paneer"))));
        mvc.perform(get("/api/foods").param("category", "fruits"))
                .andExpect(jsonPath("$[*].category", everyItem(is("fruits"))));
        mvc.perform(get("/api/foods").param("category", "pizza")).andExpect(status().isBadRequest());
    }

    @Test
    void logFromDatabaseScalesByQuantity() throws Exception {
        long roti = foodId("Roti");
        postJson("/api/log/entries", """
                {"mealType":"lunch","time":"13:15","foodId":%d,"quantity":2}""".formatted(roti))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.foodName").value("Roti / Chapati"))
                .andExpect(jsonPath("$.calories").value(240))
                .andExpect(jsonPath("$.protein").value(7.0))
                .andExpect(jsonPath("$.mealType").value("lunch"))
                .andExpect(jsonPath("$.loggedTime").value("13:15:00"));
    }

    @Test
    void summaryAddsExerciseBackAndReportsDeficit() throws Exception {
        postJson("/api/log/entries", """
                {"mealType":"breakfast","name":"Oats","calories":400,"protein":12,"quantity":1}""")
                .andExpect(status().isCreated());
        postJson("/api/log/entries", """
                {"mealType":"dinner","name":"Pizza","calories":300,"quantity":2}""")
                .andExpect(status().isCreated());
        postJson("/api/log/exercises", """
                {"name":"Run","durationMinutes":30,"caloriesBurned":250}""")
                .andExpect(status().isCreated());

        mvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value(2200))
                .andExpect(jsonPath("$.consumed").value(1000))
                .andExpect(jsonPath("$.burned").value(250))
                .andExpect(jsonPath("$.remaining").value(1450))
                .andExpect(jsonPath("$.status").value("Deficit"))
                .andExpect(jsonPath("$.protein").value(12.0))
                .andExpect(jsonPath("$.meals[?(@.mealType=='dinner')].calories").value(600));
    }

    @Test
    void updateAndDeleteEntry() throws Exception {
        String res = postJson("/api/log/entries", """
                {"mealType":"snack","name":"Chai","calories":90,"quantity":1}""")
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(res).get("id").asLong();

        mvc.perform(put("/api/log/entries/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mealType":"breakfast","name":"Chai","calories":90,"quantity":3}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calories").value(270))
                .andExpect(jsonPath("$.mealType").value("breakfast"));

        mvc.perform(delete("/api/log/entries/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/log")).andExpect(jsonPath("$.entries", hasSize(0)));
    }

    @Test
    void validationErrors() throws Exception {
        postJson("/api/log/entries", """
                {"mealType":"lunch","quantity":0}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("quantity")));

        postJson("/api/log/entries", """
                {"mealType":"lunch","quantity":1,"calories":100}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("food name")));

        postJson("/api/log/entries", """
                {"mealType":"brunch","quantity":1,"name":"x","calories":1}""")
                .andExpect(status().isBadRequest());

        postJson("/api/log/exercises", """
                {"name":"","caloriesBurned":-5}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(2)));

        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calorieGoal\":100,\"proteinGoal\":1,\"carbsGoal\":1,\"fatGoal\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profileUpdateChangesGoal() throws Exception {
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Soham","calorieGoal":1800,"proteinGoal":100,"carbsGoal":200,"fatGoal":60}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Soham"));
        mvc.perform(get("/api/summary")).andExpect(jsonPath("$.goal").value(1800));

        // restore defaults for other tests
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content("""
                {"calorieGoal":2200,"proteinGoal":140,"carbsGoal":250,"fatGoal":70}"""));
    }

    @Test
    void progressUsesSampleWeek() throws Exception {
        mvc.perform(post("/api/sample-data/week")).andExpect(jsonPath("$.daysFilled").value(7));
        mvc.perform(post("/api/sample-data/week")).andExpect(jsonPath("$.daysFilled").value(0));

        JsonNode p = json.readTree(mvc.perform(get("/api/progress").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(7)))
                .andReturn().getResponse().getContentAsString());

        // 7-day window ends today, so it contains 6 of the 7 sample days
        org.junit.jupiter.api.Assertions.assertEquals(6, p.get("loggedDays").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.now().toString(), p.get("to").asText());
        org.junit.jupiter.api.Assertions.assertTrue(p.get("averageConsumed").asInt() > 800);

        mvc.perform(get("/api/progress").param("days", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void forecastNeedsHistoryThenPredictsSevenDays() throws Exception {
        mvc.perform(get("/api/forecast")).andExpect(jsonPath("$.available").value(false));

        mvc.perform(post("/api/sample-data").param("days", "28")).andExpect(status().isOk());
        mvc.perform(get("/api/forecast").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.points", hasSize(7)))
                .andExpect(jsonPath("$.points[0].date").value(LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.points[*].predicted", everyItem(greaterThan(800))))
                .andExpect(jsonPath("$.basedOnDays").value(28));
        mvc.perform(get("/api/forecast").param("days", "30")).andExpect(status().isBadRequest());
    }

    @Test
    void suggestionsFitRemainingBudget() throws Exception {
        String res = mvc.perform(get("/api/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions", hasSize(3)))
                .andReturn().getResponse().getContentAsString();
        JsonNode s = json.readTree(res);
        int target = s.get("targetCalories").asInt();
        for (JsonNode sug : s.get("suggestions")) {
            org.junit.jupiter.api.Assertions.assertTrue(sug.get("calories").asInt() <= target);
            org.junit.jupiter.api.Assertions.assertTrue(sug.get("items").size() >= 1);
        }

        // Over budget -> light options
        postJson("/api/log/entries", """
                {"mealType":"lunch","name":"Feast","calories":3000,"quantity":1}""").andExpect(status().isCreated());
        mvc.perform(get("/api/suggestions"))
                .andExpect(jsonPath("$.message", containsString("over")))
                .andExpect(jsonPath("$.suggestions[*].calories", everyItem(lessThanOrEqualTo(150))));
    }
}
