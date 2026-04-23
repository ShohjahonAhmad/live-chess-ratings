package com.example.demo.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonthlyRatingsRequestDTO {
    public String linkToFile;
    public String date;  // Format: YYYY-MM-DD (e.g., "2026-06-01")
}
