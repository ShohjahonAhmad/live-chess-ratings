package com.example.demo.service;

import com.example.demo.batch.FideImportJob;
import com.example.demo.dto.MonthlyRatingsResponseDTO;
import com.example.demo.repository.RatingRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MonthlyRatingsService {
    @Qualifier("importPlayersJob")
    private final Job fideImportJob;
    private final JobOperator jobOperator;

    public MonthlyRatingsService(Job fideImportJob, JobOperator jobOperator) {
        this.fideImportJob = fideImportJob;
        this.jobOperator = jobOperator;
    }

    public MonthlyRatingsResponseDTO importMonthlyRatings(String fileUrl, String importDate) {
        try {
            JobParameters jobParams = new JobParametersBuilder()
                    .addString("fileUrl", fileUrl)
                    .addString("importDate", importDate)
                    .toJobParameters();
            jobOperator.start(fideImportJob, jobParams);

            return new MonthlyRatingsResponseDTO(true ,"Import job started successfully"); //how do i get number of rows added do you want me to make call to db count of monthly ratings table with given date
        } catch (Exception e) {
            return new MonthlyRatingsResponseDTO(false, e.getMessage());
        }
    }
}
