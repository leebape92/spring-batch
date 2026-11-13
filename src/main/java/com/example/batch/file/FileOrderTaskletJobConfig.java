package com.example.batch.file;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** [파일 / Tasklet] Job: fileOrderTaskletJob */
@Configuration
public class FileOrderTaskletJobConfig {

    @Bean
    public Job fileOrderTaskletJob(JobRepository jobRepository, @Qualifier("fileOrderTaskletStep") Step fileOrderTaskletStep) {
        Job job = new JobBuilder("fileOrderTaskletJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fileOrderTaskletStep) // Step fileOrderTaskletStep 메서드 실행 -> FileOrderTasklet 실행
                .build();
        return job;
    }

    @Bean
    public Step fileOrderTaskletStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, FileOrderTasklet fileOrderTasklet) {
        Step step = new StepBuilder("fileOrderTaskletStep", jobRepository)
                .tasklet(fileOrderTasklet, transactionManager)
                .build();
        return step;
    }
}
