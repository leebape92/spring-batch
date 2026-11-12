package com.example.batch.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.example.batch.common.OrderStatusRule;

/**
 * DB 배치의 Processor: 상태코드를 다음 단계로 변경한다.
 * 변경 대상이 아니면 null 을 반환 -> Writer 로 넘어가지 않는다(필터링, filterCount 증가).
 * 즉, UPDATE 는 실제로 바뀌는 주문에 대해서만 실행된다.
 */
@Component
public class DbOrderProcessor implements ItemProcessor<DbOrder, DbOrder> {

    private static final Logger log = LoggerFactory.getLogger(DbOrderProcessor.class);

    @Override
    public DbOrder process(DbOrder item) throws Exception {
        DbOrder result = null;

        String nextStatus = OrderStatusRule.next(item.getStatusCode());
        if (nextStatus != null) {
            result = new DbOrder();
            result.setOrderId(item.getOrderId());
            result.setCustomerName(item.getCustomerName());
            result.setAmount(item.getAmount());
            result.setPrevStatusCode(item.getStatusCode());
            result.setStatusCode(nextStatus);
            log.info("[DB Chunk] process : order {} {} -> {}",
                    item.getOrderId(), item.getStatusCode(), nextStatus);
        } else {
            log.info("[DB Chunk] skip    : order {} ({}) 변경 대상 아님",
                    item.getOrderId(), item.getStatusCode());
        }
        return result;
    }
}
