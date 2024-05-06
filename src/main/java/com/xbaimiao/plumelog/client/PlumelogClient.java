package com.xbaimiao.plumelog.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.xbaimiao.plumelog.client.data.BaseLogMessage;
import com.xbaimiao.plumelog.client.data.RunLogMessage;
import com.xbaimiao.plumelog.client.data.TraceLogMessage;

import java.text.SimpleDateFormat;
import java.util.Deque;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlumelogClient {

    private final String host;
    private final String env;
    private final String appName;
    private final String serverName;
    private final Gson gson = new Gson();
    private final Deque<RunLogMessage> runLogDeque = new ConcurrentLinkedDeque<>();
    private final Deque<TraceLogMessage> traceLogDeque = new ConcurrentLinkedDeque<>();
    private final ExecutorService queueThread = Executors.newSingleThreadExecutor();
    private final ExecutorService executors = Executors.newFixedThreadPool(4);

    public int signalMaxCount = 100;

    /**
     * PlumelogClient
     *
     * @param host       日志服务地址
     * @param env        环境
     * @param appName    应用名称
     * @param serverName 服务器名称
     */
    public PlumelogClient(String host, String env, String appName, String serverName) {
        this.host = host;
        this.env = env;
        this.appName = appName;
        this.serverName = serverName;
        initQueue();
    }

    private void initQueue() {
        queueThread.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!runLogDeque.isEmpty()) {
                        JsonArray jsonElements = new JsonArray();
                        RunLogMessage poll;
                        int currentCount = 0;
                        while ((poll = runLogDeque.poll()) != null) {
                            jsonElements.add(gson.toJsonTree(poll));
                            currentCount++;
                            if (currentCount >= signalMaxCount) {
                                break;
                            }
                        }

                        executors.execute(() -> {
                            String param = jsonElements.toString();
                            HttpClient.doPostBody(host + "/sendRunLog", param);
                        });
                    }
                    if (!traceLogDeque.isEmpty()) {
                        JsonArray jsonElements = new JsonArray();
                        TraceLogMessage poll;

                        int currentCount = 0;
                        while ((poll = traceLogDeque.poll()) != null) {
                            jsonElements.add(gson.toJsonTree(poll));
                            currentCount++;
                            if (currentCount >= signalMaxCount) {
                                break;
                            }
                        }
                        executors.execute(() -> {
                            String param = jsonElements.toString();
                            HttpClient.doPostBody(host + "/sendTraceLog", param);
                        });
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    printRunLog(
                            "com.xbaimiao.plumelog.client.PlumelogClient.queueThread",
                            "0",
                            e.getMessage(),
                            LogLevel.ERROR,
                            "com.xbaimiao.plumelog.client.PlumelogClient",
                            "queueThread"
                    );
                }
            }
        });
    }

    public void printRunLog(String methodName, String traceId, String content, LogLevel logLevel, String className, String threadName) {
        Long time = System.currentTimeMillis();
        RunLogMessage log = new RunLogMessage();
        log.setDtTime(time);
        log.setContent(content);
        log.setLogLevel(logLevel.name());
        log.setClassName(className);
        log.setThreadName(threadName);
        log.setDateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time));
        log.setLogType("1");

        putBaseParams(log, methodName, traceId);
        runLogDeque.add(log);
    }

    public void printTraceLog(String methodName, String traceId, Long time, String position, Integer positionNum) {
        TraceLogMessage log = new TraceLogMessage();
        log.setTime(time);
        log.setPosition(position);
        log.setPositionNum(positionNum);
        putBaseParams(log, methodName, traceId);
        traceLogDeque.add(log);
    }

    private void putBaseParams(BaseLogMessage log, String methodName, String traceId) {
        log.setAppName(appName);
        log.setEnv(env);
        log.setServerName(serverName);
        log.setMethod(methodName);
        log.setTraceId(traceId);
    }

    // demo
    public static void main(String[] args) {
        Random random = new Random();
        PlumelogClient plumelogClient = new PlumelogClient("http://localhost:8891", "java", "大厅", "localhost");
        for (int b = 0; b < 4233; b++) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < random.nextInt() + 10; i++) {
                stringBuilder.append("大师傅大势才能下载NBC卡拉瓦的\r\n");
            }
            plumelogClient.printRunLog(
                    "com.xbaimiao.plumelog.client.PlumelogClient.main",
                    "" + random.nextInt(),
                    stringBuilder.toString(),
                    LogLevel.WARN,
                    "com.xbaimiao.plumelog.client.PlumelogClient",
                    "main"
            );
        }
    }

    public void close() {
        queueThread.shutdown();
        executors.shutdown();
        runLogDeque.clear();
        traceLogDeque.clear();
    }

}
