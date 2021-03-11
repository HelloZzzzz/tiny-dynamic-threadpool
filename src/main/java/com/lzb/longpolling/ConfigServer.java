package com.lzb.longpolling;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;


/**
 * @Author : LZB
 * @Date : 2021/3/10
 * @Description : 配置服务端 配置修改之后推送给客户端
 */
@RestController
@Slf4j
@SpringBootApplication
public class ConfigServer {

    @Data
    private static class AsyncTask {
        // 长轮询请求的上下文，包含请求和响应体
        private AsyncContext asyncContext;
        // 超时标记
        private boolean timeout;

        public AsyncTask(AsyncContext asyncContext, boolean timeout) {
            this.asyncContext = asyncContext;
            this.timeout = timeout;
        }
    }


    private volatile Map<String, AsyncTask> asyncTaskMap = new ConcurrentHashMap<>();


    private ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("longPolling-timeout-checker-%d").build();
    private ScheduledExecutorService timeoutChecker = new ScheduledThreadPoolExecutor(1, threadFactory);

    /**
     *  1、监听接入点
     * @param request
     * @param response
     */
    @RequestMapping("/listener")
    public void addListener(HttpServletRequest request, HttpServletResponse response) {

        String threadPoolName = request.getParameter("threadPoolName");

        // 2、 开启异步
        AsyncContext asyncContext = request.startAsync(request, response);
        AsyncTask asyncTask = new AsyncTask(asyncContext, true);

        asyncTaskMap.put(threadPoolName, asyncTask);
        // 3、启动定时器，30s 后写入 304 响应
        timeoutChecker.schedule(() -> {
            if (asyncTask.isTimeout()) {
                asyncTaskMap.remove(threadPoolName, asyncTask);
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                asyncContext.complete();
            }
        }, 30000, TimeUnit.MILLISECONDS);
    }

    /**
     * 4、 配置发布接入点
     * @param threadPoolName
     * @param corePoolSize
     * @param maximumPoolSize
     * @return
     */
    @RequestMapping("/publishConfig")
    @SneakyThrows
    public String publishConfig(String threadPoolName, String corePoolSize, String maximumPoolSize) {
        log.info("publish configInfo dataId: [{}], corePoolSize: {},maximumPoolSize：{}", threadPoolName, corePoolSize, maximumPoolSize);
        AsyncTask asyncTask = asyncTaskMap.remove(threadPoolName);
        asyncTask.setTimeout(false);
        HttpServletResponse response = (HttpServletResponse) asyncTask.getAsyncContext().getResponse();
        response.setStatus(HttpServletResponse.SC_OK);
        Map<String,String> config = new HashMap<>(2);
        config.put("corePoolSize",corePoolSize);
        config.put("maximumPoolSize",maximumPoolSize);
        //约定格式
        response.getWriter().println(config);
        asyncTask.getAsyncContext().complete();

        return "success";
    }

    public static void main(String[] args) {
        SpringApplication.run(ConfigServer.class, args);
    }

}

