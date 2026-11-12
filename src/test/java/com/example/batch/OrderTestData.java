package com.example.batch;

import java.util.Arrays;
import java.util.List;

/** 테스트에서 공통으로 사용하는 기대값 (input/orders.csv, db/sample-data.sql 과 동일한 10건 기준) */
public final class OrderTestData {

    private OrderTestData() {
    }

    /** 상태코드를 한 단계씩 진행한 뒤의 결과 (CSV 형식, 10건) */
    public static List<String> expectedResultLines() {
        List<String> lines = Arrays.asList(
                "1,kim,ORDERED,10000",
                "2,lee,READY,20000",
                "3,park,READY,15000",
                "4,choi,READY,30000",
                "5,jung,SHIPPED,12000",
                "6,kang,SHIPPED,8000",
                "7,yoon,DONE,50000",
                "8,lim,DONE,22000",
                "9,han,DONE,17000",
                "10,seo,CANCEL,9000");
        return lines;
    }

    /** 상태코드를 한 단계씩 진행한 뒤의 주문별 상태코드 (order_id 순서) */
    public static List<String> expectedStatuses() {
        List<String> statuses = Arrays.asList(
                "ORDERED", "READY", "READY", "READY", "SHIPPED",
                "SHIPPED", "DONE", "DONE", "DONE", "CANCEL");
        return statuses;
    }
}
