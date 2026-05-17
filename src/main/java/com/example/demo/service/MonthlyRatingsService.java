package com.example.demo.service;

import com.example.demo.dto.MonthlyRatingsResponseDTO;
import com.example.demo.utils.TimeControl;
import jakarta.transaction.Transactional;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class MonthlyRatingsService {
    private final Job fideImportJob;
    private final Job changeActivenessJob;
    private final JobOperator jobOperator;

    public MonthlyRatingsService(@Qualifier("importPlayersJob") Job fideImportJob, @Qualifier("changeActiveness") Job changeActivenessJob, JobOperator jobOperator) {
        this.fideImportJob = fideImportJob;
        this.changeActivenessJob = changeActivenessJob;
        this.jobOperator = jobOperator;
    }

    @Transactional
    public MonthlyRatingsResponseDTO importMonthlyRatings(String fileUrl, String importDate) {
        try {
            JobParameters jobParams = new JobParametersBuilder()
                    .addString("fileUrl", fileUrl)
                    .addString("importDate", importDate)
                    .toJobParameters();
            jobOperator.start(fideImportJob, jobParams);

            return new MonthlyRatingsResponseDTO(true ,"Import job completed successfully");
        } catch (Exception e) {
            return new MonthlyRatingsResponseDTO(false, e.getMessage());
        }
    }

    public MonthlyRatingsResponseDTO changePlayerActiveness(String fileUrl, String timeControl) {
        try {
            validate(timeControl);
            JobParameters jobParams = new JobParametersBuilder()
                    .addString("fileUrl", fileUrl)
                    .addString("timeControl", timeControl)
                    .toJobParameters();
            jobOperator.start(changeActivenessJob, jobParams);

            return new MonthlyRatingsResponseDTO(true, "Change activeness job completed successfully");
        } catch (Exception e) {
            return new MonthlyRatingsResponseDTO(false, e.getMessage());
        }
    }

    private void validate(String timeControl) {
        try {
            TimeControl.valueOf(timeControl.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid time control: " + timeControl + ". Must be one of: STD, RAPID, BLITZ");
        }
    }
}
