# 04. 파일 배치

`file` 패키지는 `input/orders.csv`(10건)를 읽어 상태코드를 변경한 뒤 결과를 CSV 파일로 저장합니다.

| Job | 방식 | 처리 |
|-----|------|------|
| `fileOrderTaskletJob` | Tasklet | `orders.csv`를 읽고 가공하여 결과 파일 1개 생성 |
| `fileOrderChunkJob` | Chunk | `orders.csv`를 읽고 가공하여 결과 파일 1개 생성 |
| `fileOrderChunkSplitJob` | Chunk | 결과를 **원하는 건수마다 파일을 나누어** 저장 |

- Tasklet: `execute()` 한 번에 파일 읽기 → 상태코드 변경 → 파일 쓰기를 전부 직접 처리
- Chunk: Reader(`FlatFileItemReader`) → Processor(`FileOrderProcessor`) → Writer(`FlatFileItemWriter`)
- 파일 배치는 변경 대상이 아닌 행도 상태코드를 그대로 두고 **10건 전부** 결과 파일에 기록합니다.

## 파일 분리 저장 (`fileOrderChunkSplitJob`)

`file` 패키지는 `db` 패키지와 다르게, **파일의 양이 많은 경우를 대비해**
원하는 개수만큼 저장한 이후에 파일을 나누는 로직을 추가했습니다.

- `MultiResourceItemWriter` : 파일을 나누는 **관리자**
- `FlatFileItemWriter` : 실제로 쓰는 **실무자** (delegate)
- `ResourceSuffixCreator` : 파일명 뒤에 붙는 번호(`-1.csv`, `-2.csv` ...)를 만든다

```java
MultiResourceItemWriter<FileOrder> writer = new MultiResourceItemWriterBuilder<FileOrder>()
        .name("fileOrderSplitWriter")
        .resource(new FileSystemResource(BASE_PATH))
        .itemCountLimitPerResource(ITEMS_PER_FILE)   // 이 개수만큼 쓰면 파일을 닫고 다음 파일로
        .resourceSuffixCreator(suffixCreator)        // order-1.csv, order-2.csv ...
        .delegate(fileOrderSplitDelegateWriter)
        .build();
```

샘플 설정은 `chunk 2건 / 파일당 4건` 이므로 입력 10건은 아래처럼 3개 파일로 나뉩니다.

| 파일 | 건수 |
|------|------|
| `build/output/order-split/order-1.csv` | 4건 |
| `build/output/order-split/order-2.csv` | 4건 |
| `build/output/order-split/order-3.csv` | 2건 |

## 구현하면서 궁금했던 점

### Q2. 파일을 나눠서 저장할 때 Writer가 2개인 이유는? (`MultiResourceItemWriter` / `FlatFileItemWriter`)

역할을 **관리자**와 **실무자**로 나눠서 이해하면 쉽습니다.

| 클래스 | 역할 | 하는 일 |
|--------|------|---------|
| `MultiResourceItemWriter` | **관리자** | 몇 건마다 파일을 나눌지 관리하고, 새 파일이 필요할 때 파일명(resource)을 지정해 준다 |
| `FlatFileItemWriter` | **실무자** | 실제로 파일에 한 줄씩 **쓰는** 작업을 담당한다 (`delegate`) |

그래서 `FlatFileItemWriter`를 만들 때는 `resource`를 지정하지 않습니다.
**파일마다 쓸 resource는 `MultiResourceItemWriter`가 지정해 줍니다.**

```java
// 실제 쓰기를 담당하는 Writer (resource 는 MultiResourceItemWriter 가 파일마다 지정해 준다)
FlatFileItemWriter<FileOrder> fileOrderSplitDelegateWriter = new FlatFileItemWriterBuilder<FileOrder>()
        .name("fileOrderSplitDelegateWriter")
        .delimited().delimiter(",")
        .names(new String[] {"orderId", "customerName", "statusCode", "amount"})
        .build();
```

### Q3. 나뉜 파일의 이름은 어떻게 붙을까? (`ResourceSuffixCreator`)

`ResourceSuffixCreator`는 `MultiResourceItemWriter`가 여러 개의 출력 파일을 분할 생성할 때,
**각 파일 이름 뒤에 붙는 접미사**를 만들어 줍니다. (예: `order` + `-1.csv`)

```java
ResourceSuffixCreator suffixCreator = new ResourceSuffixCreator() {
    @Override
    public String getSuffix(int index) {
        return "-" + index + ".csv";
    }
};
```

동작 순서는 다음과 같습니다.

1. `itemCountLimitPerResource` 에 지정한 개수만큼 파일에 쓰면 현재 파일을 **닫는다**.
2. `MultiResourceItemWriter` 내부의 `resourceIndex` 필드가 `++` 된다.
3. 증가한 `index`로 `ResourceSuffixCreator`가 동작하여 **파일명이 채번**된다. (`order-1.csv` → `order-2.csv` → ...)
4. 새 파일을 열고 이어서 쓴다.
