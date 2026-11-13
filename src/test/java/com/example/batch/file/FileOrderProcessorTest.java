package com.example.batch.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Processor 단위 테스트 (스프링 없이 1건 입력 -> 1건 출력) */
class FileOrderProcessorTest {

//    private final FileOrderProcessor processor = new FileOrderProcessor();
//
//    @Test
//    @DisplayName("PAID 는 READY 로 변경된다")
//    void paidToReady() throws Exception {
//        FileOrder input = createOrder(2L, "PAID");
//
//        FileOrder output = processor.process(input);
//
//        assertThat(output.getOrderId()).isEqualTo(2L);
//        assertThat(output.getStatusCode()).isEqualTo("READY");
//    }
//
//    @Test
//    @DisplayName("변경 대상이 아닌 상태(DONE)는 그대로 유지된다")
//    void doneStaysDone() throws Exception {
//        FileOrder output = processor.process(createOrder(9L, "DONE"));
//
//        assertThat(output.getStatusCode()).isEqualTo("DONE");
//    }
//
//    @Test
//    @DisplayName("원본 객체는 변경하지 않는다")
//    void doesNotChangeInput() throws Exception {
//        FileOrder input = createOrder(3L, "PAID");
//
//        processor.process(input);
//
//        assertThat(input.getStatusCode()).isEqualTo("PAID");
//    }
//
//    private FileOrder createOrder(Long orderId, String statusCode) {
//        FileOrder order = new FileOrder();
//        order.setOrderId(orderId);
//        order.setCustomerName("tester");
//        order.setStatusCode(statusCode);
//        order.setAmount(1000L);
//        return order;
//    }
}
