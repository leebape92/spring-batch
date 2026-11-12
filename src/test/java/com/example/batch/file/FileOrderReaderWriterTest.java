package com.example.batch.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;

/** Reader 는 "1건씩" 읽고, Writer 는 "묶음(Chunk)"을 한 번에 쓴다는 점을 확인한다. */
class FileOrderReaderWriterTest {

    private final FileOrderChunkJobConfig config = new FileOrderChunkJobConfig();

    @Test
    @DisplayName("Reader 는 CSV 를 1건씩 읽고, 끝나면 null 을 반환한다")
    void readerReadsOneByOne() throws Exception {
        FlatFileItemReader<FileOrder> reader = config.fileOrderReader();
        reader.open(new ExecutionContext());

        List<FileOrder> orders = new ArrayList<FileOrder>();
        FileOrder order = reader.read();
        while (order != null) {
            orders.add(order);
            order = reader.read();
        }
        reader.close();

        assertThat(orders).hasSize(10);
        assertThat(orders.get(0).getOrderId()).isEqualTo(1L);
        assertThat(orders.get(0).getCustomerName()).isEqualTo("kim");
        assertThat(orders.get(0).getStatusCode()).isEqualTo("ORDERED");
        assertThat(orders.get(0).getAmount()).isEqualTo(10000L);
        assertThat(orders.get(9).getStatusCode()).isEqualTo("CANCEL");
    }

    @Test
    @DisplayName("Writer 는 Chunk(여러 건)를 받아 한 번에 파일에 쓴다")
    void writerWritesChunk() throws Exception {
        FlatFileItemWriter<FileOrder> writer = config.fileOrderWriter();
        writer.open(new ExecutionContext());

        List<FileOrder> items = new ArrayList<FileOrder>();
        items.add(createOrder(1L, "kim", "READY", 100L));
        items.add(createOrder(2L, "lee", "DONE", 200L));
        writer.write(new Chunk<FileOrder>(items));
        writer.close();

        List<String> lines = Files.readAllLines(Paths.get(FileOrderChunkJobConfig.OUTPUT_PATH), StandardCharsets.UTF_8);
        assertThat(lines).containsExactly("1,kim,READY,100", "2,lee,DONE,200");
    }

    private FileOrder createOrder(Long orderId, String name, String status, Long amount) {
        FileOrder order = new FileOrder();
        order.setOrderId(orderId);
        order.setCustomerName(name);
        order.setStatusCode(status);
        order.setAmount(amount);
        return order;
    }
}
