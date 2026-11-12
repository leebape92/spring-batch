package com.example.batch.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Tasklet 방식: SQL 두 개로 상태코드를 "한 번에" 변경한다. (집합 처리)
 *   1) 변경 전/후 상태를 이력 테이블에 INSERT
 *   2) 주문 테이블 UPDATE
 * 두 SQL 은 같은 트랜잭션이라 둘 다 성공하거나 둘 다 롤백된다.
 */
@Component
public class DbOrderTasklet implements Tasklet {
	
    private static final Logger log = LoggerFactory.getLogger(DbOrderTasklet.class);

    private static final String INSERT_HISTORY_SQL =
            "INSERT INTO TB_ORDER_STATUS_HIST (order_id, before_status, after_status) "
            + "SELECT order_id, status_code, "
            + "CASE status_code WHEN 'PAID' THEN 'READY' WHEN 'READY' THEN 'SHIPPED' WHEN 'SHIPPED' THEN 'DONE' END "
            + "FROM TB_ORDER WHERE status_code IN ('PAID', 'READY', 'SHIPPED')";

    private static final String UPDATE_STATUS_SQL =
            "UPDATE TB_ORDER SET "
            + "status_code = CASE status_code WHEN 'PAID' THEN 'READY' WHEN 'READY' THEN 'SHIPPED' WHEN 'SHIPPED' THEN 'DONE' END, "
            + "updated_at = CURRENT_TIMESTAMP "
            + "WHERE status_code IN ('PAID', 'READY', 'SHIPPED')";

    private final JdbcTemplate jdbcTemplate;

    public DbOrderTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        int historyCount = jdbcTemplate.update(INSERT_HISTORY_SQL);
        int updatedCount = jdbcTemplate.update(UPDATE_STATUS_SQL);
        log.info("[DB Tasklet] 이력 {} 건 저장, 주문 {} 건 상태 변경", historyCount, updatedCount);

        RepeatStatus status = RepeatStatus.FINISHED;
        return status;
    }
}
