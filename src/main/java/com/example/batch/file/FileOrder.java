package com.example.batch.file;

/** 파일(CSV) 한 줄을 담는 객체: orderId,customerName,statusCode,amount */
public class FileOrder {

    private Long orderId;
    private String customerName;
    private String statusCode;
    private Long amount;

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

    @Override
    public String toString() {
        return "FileOrder{orderId=" + orderId + ", customerName='" + customerName
                + "', statusCode='" + statusCode + "', amount=" + amount + "}";
    }
}
