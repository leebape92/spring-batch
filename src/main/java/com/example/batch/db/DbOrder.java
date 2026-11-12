package com.example.batch.db;

/** TB_ORDER 한 행을 담는 객체 (prevStatusCode 는 이력 저장용: 변경 전 상태코드) */
public class DbOrder {

    private Long orderId;
    private String customerName;
    private String statusCode;
    private Long amount;
    private String prevStatusCode;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getPrevStatusCode() {
        return prevStatusCode;
    }

    public void setPrevStatusCode(String prevStatusCode) {
        this.prevStatusCode = prevStatusCode;
    }

    @Override
    public String toString() {
        return "DbOrder{orderId=" + orderId + ", statusCode='" + statusCode
                + "', prevStatusCode='" + prevStatusCode + "'}";
    }
}
