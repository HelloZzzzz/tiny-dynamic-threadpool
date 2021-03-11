package com.lzb.threadpool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @Author : LZB
 * @Date : 2021/3/9
 * @Description : 监控线程池
 */
@Slf4j
public final class MonitorThreadPool implements Runnable {

    /**
     * 定义线程池使用的阀值
     */
    private static final double ALARM_PERCENT = 0.80;

    private static final Map<String, ThreadPoolExecutor> THREAD_POOLS = new ConcurrentHashMap<>(16);


    private static final MonitorThreadPool MONITOR_THREAD_POOL = new MonitorThreadPool();


    private MonitorThreadPool() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("-MonitorThreadPool-").build();
        ScheduledExecutorService monitorThreadPool = new ScheduledThreadPoolExecutor(1, threadFactory);
        // 每隔3秒打印线程使用情况
        monitorThreadPool.scheduleWithFixedDelay(this, 1, 3, TimeUnit.SECONDS);
    }

    public static MonitorThreadPool getMonitorThreadPool() {
        return MONITOR_THREAD_POOL;
    }


    public void addMonitoredThreadPool(String poolName, ThreadPoolExecutor executor) {
        THREAD_POOLS.put(poolName, executor);
    }

    public ThreadPoolExecutor getMonitoredThreadPool(String poolName) {
        return THREAD_POOLS.get(poolName);
    }

    @Override
    public void run() {
        THREAD_POOLS.forEach((key, value) -> {
            /*
            线程池活跃度计算公式为：线程池活跃度 = activeCount/maximumPoolSize。
            这个公式代表当活跃线程数趋向于maximumPoolSize的时候，代表线程负载趋高。
             */
            final int activeCount = value.getActiveCount();
            final int maximumPoolSize = value.getMaximumPoolSize();
            double usedPercent = activeCount / (maximumPoolSize * 1.0);
            if (usedPercent > ALARM_PERCENT) {
                log.error("{}线程池超出警戒线! [{}/{}:{}%] ", key, activeCount, maximumPoolSize, usedPercent * 100);
            } else {
                log.info("{}线程池活跃度: [{}/{}:{}%]", key, activeCount, maximumPoolSize, usedPercent * 100);
            }
        });

    }
}