package com.example.demo.controller;

import com.example.demo.dto.MonthlyRatingsResponseDTO;
import com.example.demo.service.MonthlyRatingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonthlyRatingsController.class)
public class MonthlyRatingsTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonthlyRatingsService monthlyRatingsService;

    private static final String POST_REQUEST_JSON = """
                        {
                            "linkToFile": "https://example.com",
                            "date": "2024-06-01"
                        }
                        """;


    @Test
    public void testThatPostMonthlyRatingsReturns200Ok() throws Exception {
        when(monthlyRatingsService.importMonthlyRatings(any(), any()))
                .thenReturn(new MonthlyRatingsResponseDTO(true, "Import successful"));

        mockMvc.perform(post("/monthly-ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(POST_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Import successful"));
    }

    @Test
    public void testThatPostMonthlyRatingsReturns400BadRequest() throws Exception {
        when(monthlyRatingsService.importMonthlyRatings(any(), any()))
                .thenReturn(new MonthlyRatingsResponseDTO(false, "Import failed"));

        mockMvc.perform(post("/monthly-ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(POST_REQUEST_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Import failed"));
    }

    @Test
    public void testThatPostMonthlyRatingsReturns500InternalServerError() throws Exception {
        when(monthlyRatingsService.importMonthlyRatings(any(), any()))
                .thenThrow(new RuntimeException("Unexpected Error"));

        mockMvc.perform(post("/monthly-ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(POST_REQUEST_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unexpected Error"));
    }

    @Test void testThatPostMonthlyRatingsValidatesRequestBody() throws Exception {
        mockMvc.perform(post("/monthly-ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "linkToFile": ""
                        }
                        """))
                .andExpect(status().isInternalServerError());

        mockMvc.perform(post("/monthly-ratings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
