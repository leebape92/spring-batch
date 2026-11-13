package com.example.batch.db;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * [DB / Chunk] Job: dbOrderChunkJob
 *   Reader    : TB_ORDER 를 1건씩 읽는다
 *   Processor : 상태코드를 다음 단계로 변경 (대상 아니면 null -> 제외)
 *   Writer    : chunk(2건) 단위로 [TB_ORDER UPDATE + TB_ORDER_STATUS_HIST INSERT] 를 한 트랜잭션으로 실행
 */
@Configuration
public class DbOrderChunkJobConfig {

    private static final int CHUNK_SIZE = 2;

    @Bean
    public Job dbOrderChunkJob(JobRepository jobRepository, @Qualifier("dbOrderChunkStep") Step dbOrderChunkStep) {
        Job job = new JobBuilder("dbOrderChunkJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(dbOrderChunkStep)
                .build();
        return job;
    }

    @Bean
    public Step dbOrderChunkStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, JdbcCursorItemReader<DbOrder> dbOrderReader,
                                 DbOrderProcessor dbOrderProcessor, CompositeItemWriter<DbOrder> dbOrderWriter) {
        Step step = new StepBuilder("dbOrderChunkStep", jobRepository)
                .<DbOrder, DbOrder>chunk(CHUNK_SIZE, transactionManager)
                .reader(dbOrderReader)
                .processor(dbOrderProcessor)
                .writer(dbOrderWriter)
                .build();
        return step;
    }

    /** 읽기: SELECT 결과를 1건씩 DbOrder 로 변환 (order_id -> orderId 로 자동 매핑) */
    
    // 테이블을 조회하면 여러개의 행이 존재하는데, 데이터를 1개씩 읽어오는 원리
    // JdbcCursorItemReader
    //	• 커서 기반 조회: JDBC의 ResultSet을 이용하여 쿼리를 한 번 실행한 뒤, 스트리밍처럼 커서를 한 칸씩(Row 단위로) 이동시키며 데이터를 읽어옵니다.
    //	• 별도의 Connection 유지: 배치 처리가 시작되어 끝날 때까지(Read가 완료될 때까지) 데이터베이스 커넥션이 계속 연결된 상태를 유지합니다.
    @Bean
    public JdbcCursorItemReader<DbOrder> dbOrderReader(DataSource dataSource) {
        JdbcCursorItemReader<DbOrder> reader = new JdbcCursorItemReaderBuilder<DbOrder>()
                .name("dbOrderReader")
                .dataSource(dataSource)
                .sql("SELECT order_id, customer_name, status_code, amount FROM TB_ORDER ORDER BY order_id")
                .rowMapper(new BeanPropertyRowMapper<DbOrder>(DbOrder.class))
                .build();
        
        return reader;
    }

    /** 쓰기: 주문 UPDATE + 이력 INSERT 를 순서대로 실행하는 Writer */
    @Bean
    public CompositeItemWriter<DbOrder> dbOrderWriter(DataSource dataSource) {
        JdbcBatchItemWriter<DbOrder> statusUpdateWriter = new JdbcBatchItemWriterBuilder<DbOrder>()
                .dataSource(dataSource)
                .sql("UPDATE TB_ORDER SET status_code = :statusCode, updated_at = CURRENT_TIMESTAMP "+ "WHERE order_id = :orderId")
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .build();
        statusUpdateWriter.afterPropertiesSet(); // ItemPreparedStatementSetter 초기화 NullPoint 방지
        

        JdbcBatchItemWriter<DbOrder> historyInsertWriter = new JdbcBatchItemWriterBuilder<DbOrder>()
                .dataSource(dataSource)
                .sql("INSERT INTO TB_ORDER_STATUS_HIST (order_id, before_status, after_status) "+ "VALUES (:orderId, :prevStatusCode, :statusCode)")
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .build();
        historyInsertWriter.afterPropertiesSet(); // ItemPreparedStatementSetter 초기화 NullPoint 방지

        List<ItemWriter<? super DbOrder>> delegates = new ArrayList<ItemWriter<? super DbOrder>>();
        delegates.add(statusUpdateWriter);
        delegates.add(historyInsertWriter);

        CompositeItemWriter<DbOrder> writer = new CompositeItemWriterBuilder<DbOrder>()
                .delegates(delegates)
                .build();
        return writer;
    }
    
}
