package com.example.batch.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderStatusRuleTest {

    @Test
    @DisplayName("PAID -> READY -> SHIPPED -> DONE 순서로 한 단계씩 진행한다")
    void nextStatus() {
        assertThat(OrderStatusRule.next("PAID")).isEqualTo("READY");
        assertThat(OrderStatusRule.next("READY")).isEqualTo("SHIPPED");
        assertThat(OrderStatusRule.next("SHIPPED")).isEqualTo("DONE");
    }

    @Test
    @DisplayName("ORDERED / DONE / CANCEL / 알 수 없는 코드는 변경 대상이 아니다(null)")
    void noNextStatus() {
        assertThat(OrderStatusRule.next("ORDERED")).isNull();
        assertThat(OrderStatusRule.next("DONE")).isNull();
        assertThat(OrderStatusRule.next("CANCEL")).isNull();
        assertThat(OrderStatusRule.next("UNKNOWN")).isNull();
    }
}
