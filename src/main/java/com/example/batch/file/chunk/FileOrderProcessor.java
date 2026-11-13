package com.example.batch.file.chunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.example.batch.common.OrderStatusRule;
import com.example.batch.file.FileOrder;

/**
 * 파일 배치의 Processor: 상태코드를 다음 단계로 변경한다.
 * 결과 파일에는 모든 행을 쓰므로, 변경 대상이 아닌 행은 상태코드를 그대로 둔다.
 */
@Component
public class FileOrderProcessor implements ItemProcessor<FileOrder, FileOrder> {

    private static final Logger log = LoggerFactory.getLogger(FileOrderProcessor.class);

    @Override
    public FileOrder process(FileOrder item) throws Exception {
        String nextStatus = OrderStatusRule.next(item.getStatusCode());
        if (nextStatus == null) {
            nextStatus = item.getStatusCode();
        }

        FileOrder result = new FileOrder();
        result.setOrderId(item.getOrderId());
        result.setCustomerName(item.getCustomerName());
        result.setStatusCode(nextStatus);
        result.setAmount(item.getAmount());

        log.info("[File Chunk] process : order {} {} -> {}",
                item.getOrderId(), item.getStatusCode(), nextStatus);
        return result;
    }
}
