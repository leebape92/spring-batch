package com.example.batch.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Processor 단위 테스트 (스프링/DB 없이) */
class DbOrderProcessorTest {

    private final DbOrderProcessor processor = new DbOrderProcessor();

    @Test
    @DisplayName("PAID 는 READY 로 변경되고, 변경 전 상태가 prevStatusCode 에 담긴다")
    void paidToReady() throws Exception {
        DbOrder output = processor.process(createOrder(2L, "PAID"));

        assertThat(output).isNotNull();
        assertThat(output.getStatusCode()).isEqualTo("READY");
        assertThat(output.getPrevStatusCode()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("변경 대상이 아닌 상태(DONE, CANCEL, ORDERED)는 null 을 반환한다 (필터링)")
    void notTargetReturnsNull() throws Exception {
        assertThat(processor.process(createOrder(9L, "DONE"))).isNull();
        assertThat(processor.process(createOrder(10L, "CANCEL"))).isNull();
        assertThat(processor.process(createOrder(1L, "ORDERED"))).isNull();
    }

    private DbOrder createOrder(Long orderId, String statusCode) {
        DbOrder order = new DbOrder();
        order.setOrderId(orderId);
        order.setCustomerName("tester");
        order.setStatusCode(statusCode);
        order.setAmount(1000L);
        return order;
    }
}
