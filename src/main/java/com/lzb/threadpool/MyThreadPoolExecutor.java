package com.lzb.threadpool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author : LZB
 * @Date : 2021/3/9
 * @Description :  阿里巴巴规范中 使用ThreadPoolExecutor创建线程池
 */
@Slf4j
public final class MyThreadPoolExecutor extends ThreadPoolExecutor {

    private static int corePoolSize = 2;
    private static int maximumPoolSize = 4;
    private static long keepAliveTime = 10;

    private static TimeUnit unit = TimeUnit.SECONDS;
    private static BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1);
    private static UserThreadRejectedExecutionHandler userThreadRejectedExecutionHandler = new UserThreadRejectedExecutionHandler();


    private static final MonitorThreadPool MONITORTHREADPOOL = MonitorThreadPool.getMonitorThreadPool();


    public MyThreadPoolExecutor(String threadPoolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new ThreadFactoryBuilder().setNameFormat(threadPoolName).build(), userThreadRejectedExecutionHandler);
        MONITORTHREADPOOL.addMonitoredThreadPool(threadPoolName, this);
    }

    public MyThreadPoolExecutor(String threadPoolName,int corePoolSize, int maximumPoolSize) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new ThreadFactoryBuilder().setNameFormat(threadPoolName).build(), userThreadRejectedExecutionHandler);
        MONITORTHREADPOOL.addMonitoredThreadPool(threadPoolName, this);
    }

    private static class UserThreadRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            String msg = String.format("Thread pool is EXHAUSTED!" +
                            " Thread Name: %s, Pool Size: %d (active: %d, core: %d, max: %d, largest: %d), Task: %d (completed: "
                            + "%d)," +
                            " Executor status:(isShutdown:%s, isTerminated:%s, isTerminating:%s)",
                    Thread.currentThread().getName(), e.getPoolSize(), e.getActiveCount(), e.getCorePoolSize(), e.getMaximumPoolSize(),
                    e.getLargestPoolSize(),
                    e.getTaskCount(), e.getCompletedTaskCount(), e.isShutdown(), e.isTerminated(), e.isTerminating());
            log.error("UserThreadRejectedExecutionHandler:{}", msg);
//            throw new RejectedExecutionException(msg);
        }
    }

    @Data
    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger nextId = new AtomicInteger(1);


        public NamedThreadFactory(String whatFeatureOfGroup) {
            this.namePrefix = "From UserThreadFactory's " + whatFeatureOfGroup + "-Worker-";
        }

        @Override
        public Thread newThread(Runnable task) {
            String name = namePrefix + nextId.getAndIncrement();
            Thread thread = new Thread(task);
            thread.setName(name);
            return thread;
        }
    }



}
