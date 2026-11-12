package com.example.batch.common;

/**
 * 주문 상태코드 변경 규칙 (파일 배치 / DB 배치가 함께 사용).
 *
 *   PAID(결제완료) -> READY(배송준비) -> SHIPPED(배송중) -> DONE(배송완료)
 *   ORDERED / DONE / CANCEL 은 더 이상 진행할 다음 단계가 없다.
 */
public final class OrderStatusRule {

    private OrderStatusRule() {}
	
    public static final String ORDERED = "ORDERED";
    public static final String PAID = "PAID";
    public static final String READY = "READY";
    public static final String SHIPPED = "SHIPPED";
    public static final String DONE = "DONE";
    public static final String CANCEL = "CANCEL";

    /**
     * @return 다음 상태코드. 변경 대상이 아니면 null
     */
    public static String next(String statusCode) {
        String nextStatus = null;

        if (statusCode != null) {
            switch (statusCode) {
                case PAID:
                    nextStatus = READY;
                    break;
                case READY:
                    nextStatus = SHIPPED;
                    break;
                case SHIPPED:
                    nextStatus = DONE;
                    break;
                default:
                    nextStatus = null;
                    break;
            }
        }

        return nextStatus;
    }
}
