package com.example.batch.db.tasklet;

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

/** [DB / Tasklet] Job: dbOrderTaskletJob */
@Configuration
public class DbOrderTaskletJobConfig {

    @Bean
    public Job dbOrderTaskletJob(JobRepository jobRepository, @Qualifier("dbOrderTaskletStep") Step dbOrderTaskletStep) {
        Job job = new JobBuilder("dbOrderTaskletJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(dbOrderTaskletStep)
                .build();
        return job;
    }

    @Bean
    public Step dbOrderTaskletStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, DbOrderTasklet dbOrderTasklet) {
        Step step = new StepBuilder("dbOrderTaskletStep", jobRepository)
                .tasklet(dbOrderTasklet, transactionManager)
                .build();
        return step;
    }
}
