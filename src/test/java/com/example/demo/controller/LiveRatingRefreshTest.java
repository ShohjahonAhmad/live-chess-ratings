package com.example.demo.controller;

import com.example.demo.service.LiveRatingRefreshService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(LiveRatingRefresh.class)
public class LiveRatingRefreshTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LiveRatingRefreshService liveRatingRefreshService;

    @Test
    public void testThatRefreshLiveRatingsReturns200Ok() throws Exception {
        String importDate = "2026-01-01";
        String message = "Live ratings refreshed successfully for period: " + importDate;
        doNothing().when(liveRatingRefreshService).refreshLiveRatings(any());

        mockMvc.perform(post("/refresh-live-ratings")
                .param("importDate", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    public void testThatRefreshLiveRatingsReturns500InternalServerError() throws Exception {
        String importDate = "2026-01-01";
        String message = "Error refreshing live ratings";
        doThrow(new RuntimeException(message)).when(liveRatingRefreshService).refreshLiveRatings(any());

        mockMvc.perform(post("/refresh-live-ratings")
                .param("importDate", importDate))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    public void testThatRefreshLiveRatingsReturns200OkWithNoParam() throws Exception {
        doNothing().when(liveRatingRefreshService).refreshLiveRatings(any());

        mockMvc.perform(post("/refresh-live-ratings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Live ratings refreshed successfully for period: " + java.time.LocalDate.now().withDayOfMonth(1)));
    }

    //
}
