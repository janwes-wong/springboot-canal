package com.janwes.canal.config;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author Janwes
 * @version 1.0
 * @package com.janwes.canal.config
 * @date 2021/9/13 15:44
 * @description
 */
@Slf4j
@Component
public class CanalClient implements ApplicationRunner {

    private final Queue<String> SLQ_QUEUE = new ConcurrentLinkedDeque<>();

    @Resource
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("===> start connect to canal-server......");
        CanalConnector connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress("127.0.0.1", 11111),
                "example", "", "");
        int batchSize = 1000;
        int emptyCount = 0;
        try {
            // 连接
            connector.connect();
            log.info("===> connect to database success......");
            // 订阅所有数据库表
            connector.subscribe(".*\\..*");
            // 订阅指定数据库 例：订阅test_data数据库所有表
            //connector.subscribe("test_data\\..*");
            // 回滚
            connector.rollback();
            int totalEmptyCount = 200;
            while (emptyCount < totalEmptyCount) {
                // 尝试从master拉取数据batchSize条记录
                Message message = connector.getWithoutAck(batchSize);// 获取指定数量的数据
                long batchId = message.getId();
                int size = message.getEntries().size();
                if (batchId == -1 || size == 0) {
                    emptyCount++;
                    log.info("empty count : " + emptyCount);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log.info("===> " + String.format("message [batchId=%s,size=%s] ", batchId, size));
                } else {
                    emptyCount = 0;
                    dataHandle(message.getEntries());
                }
                connector.ack(batchId); // 提交确认
                // connector.rollback(batchId); // 处理失败, 回滚数据
            }
            log.info("empty too many times, exit");
        } catch (Exception e) {
            connector.rollback(); // 处理失败, 回滚数据
            log.error("===> 服务异常,连接中断..." + e);
        } finally {
            connector.disconnect();
            log.info("===> 成功断开连接...");
        }
    }

    private void dataHandle(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            // 开启/关闭事务的实体类跳过
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN
                    || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }
            // RowChange对象，包含了一行数据变化的所有特征
            CanalEntry.RowChange rowChange;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                // 获取数据的操作类型 update/insert/delete
                CanalEntry.EventType eventType = rowChange.getEventType();
                log.info("===> " + String.format("binlog[%s:%s] , name[%s,%s] , eventType : %s",
                        entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                        entry.getHeader().getSchemaName(), entry.getHeader().getTableName(),
                        eventType));
                // 判断是否是DDL语句
                if (rowChange.getIsDdl()) {
                    log.info("===> execute DDL statement is true,sql:{}", rowChange.getSql());
                }
                // 获取RowChange对象里的每一行数据实现业务处理
                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                    // 删除语句
                    if (eventType == CanalEntry.EventType.DELETE) {
                        collectLog(rowData.getBeforeColumnsList());
                        // 新增语句
                    } else if (eventType == CanalEntry.EventType.INSERT) {
                        collectLog(rowData.getAfterColumnsList());
                        // 更新语句
                    } else {
                        log.info("===> update data before.....");
                        collectLog(rowData.getBeforeColumnsList());
                        log.info("===> update data after.....");
                        collectLog(rowData.getAfterColumnsList());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void collectLog(List<CanalEntry.Column> columns) {
        for (CanalEntry.Column column : columns) {
            log.info("===> columnName:{},columnValue:{},update:{}", column.getName(), column.getValue(), column.getUpdated());
        }
    }
}
