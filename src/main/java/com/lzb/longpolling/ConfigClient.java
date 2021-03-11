package com.lzb.longpolling;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lzb.threadpool.MonitorThreadPool;
import com.lzb.threadpool.MyThreadPoolExecutor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author : LZB
 * @Date : 2021/3/10
 * @Description : 配置客户端 长轮询服务端获取配置
 */
@Slf4j
public class ConfigClient implements Runnable {

    private CloseableHttpClient httpClient;
    private ThreadPoolExecutor shouldTestThreadPoolExecutor;
    private RequestConfig requestConfig;

    private static MonitorThreadPool monitorThreadPool = MonitorThreadPool.getMonitorThreadPool();


    public ConfigClient() {
        this.httpClient = HttpClientBuilder.create().build();
        // 1、httpClient 客户端超时时间要大于长轮询约定的超时时间
        this.requestConfig = RequestConfig.custom().setSocketTimeout(35000).build();
    }

    public void setShouldTestThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor){
        shouldTestThreadPoolExecutor = threadPoolExecutor;
    }


    @SneakyThrows
    public void longPolling(String url, String threadPoolName) {
        String requestUrl = url + "?threadPoolName=" + threadPoolName;
        HttpGet request = new HttpGet(requestUrl);
        CloseableHttpResponse response = httpClient.execute(request);
        switch (response.getStatusLine().getStatusCode()) {
            case 200: {
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                response.close();
                String configInfo = result.toString();
                log.info("threadPoolName: [{}] changed, receive configInfo: {}", threadPoolName, configInfo);

                try {
                    //当有参数没有设置时会导致异常
                    Gson gson = new Gson();
                    Map<String, String> map = gson.fromJson(configInfo, new TypeToken<Map<String, String>>() {}.getType());
                    ThreadPoolExecutor monitoredThreadPool = monitorThreadPool.getMonitoredThreadPool(threadPoolName);
                    int corePoolSize = Integer.parseInt(map.get("corePoolSize"));
                    int maximumPoolSize = Integer.parseInt(map.get("maximumPoolSize"));
                    //简单参数校验
                    if (maximumPoolSize >= corePoolSize) {
                        monitoredThreadPool.setCorePoolSize(corePoolSize);
                        monitoredThreadPool.setMaximumPoolSize(maximumPoolSize);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    longPolling(url, threadPoolName);

                }
                break;


            }
            // 2、 304 响应码标记配置未变更
            case 304: {
                log.info("longPolling threadPoolName: [{}] once finished, configInfo is unchanged, longPolling again", threadPoolName);
                longPolling(url, threadPoolName);
                break;
            }
            default: {
                throw new RuntimeException("unExcepted HTTP status code");
            }
        }

    }

    public static void main(String[] args)  {
        ThreadPoolExecutor liaowei = new MyThreadPoolExecutor("busy");
        ThreadPoolExecutor dudu = new MyThreadPoolExecutor("empty");
        ThreadPoolExecutor main = new MyThreadPoolExecutor("listenMain", 4, 4);
        //3、对threadPoolName进行配置监听
        main.execute(() -> {
            ConfigClient configClient = new ConfigClient();
            configClient.longPolling("http://127.0.0.1:8080/listener", "liaowei");
        });


        main.execute(() -> {
            ConfigClient configClient = new ConfigClient();
            configClient.longPolling("http://127.0.0.1:8080/listener", "dudu");
        });

        main.execute(() -> {
            //我听我自己
            ConfigClient configClient = new ConfigClient();
            configClient.longPolling("http://127.0.0.1:8080/listener", "listenMain");
        });


        // 关闭掉较多的日志
        Logger logger = (Logger) LoggerFactory.getLogger("org.apache.http");
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);

        ConfigClient configClient = new ConfigClient();
        configClient.setShouldTestThreadPoolExecutor(liaowei);
        new Thread(configClient).start();




    }

    @Override
    public void run() {
        if (null == shouldTestThreadPoolExecutor ){
            return;
        }
        //提交任务
        for (int i = 0; i < 100; i++) {
            try {
                TimeUnit.SECONDS.sleep(1L);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            shouldTestThreadPoolExecutor.execute(new MyTask());
        }
    }

    private static class MyTask implements Runnable {

        @Override
        public void run() {
            try {
                TimeUnit.SECONDS.sleep(10L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
