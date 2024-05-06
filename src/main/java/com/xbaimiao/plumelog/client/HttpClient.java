package com.xbaimiao.plumelog.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpClient {

    public static String doPostBody(String url, String param) {
        HttpURLConnection httpClient = null;
        StringBuilder result = new StringBuilder();

        try {
            // 创建连接
            URL urlObj = new URL(url);
            httpClient = (HttpURLConnection) urlObj.openConnection();

            // 设置请求方法为POST
            httpClient.setRequestMethod("POST");

            // 发送POST请求必须设置下面两行
            httpClient.setDoOutput(true);
            httpClient.setDoInput(true);

            // 设置请求属性
            httpClient.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            httpClient.setConnectTimeout(35000);
            httpClient.setReadTimeout(60000);

            // 获取连接的输出流
            OutputStreamWriter out = new OutputStreamWriter(httpClient.getOutputStream(), StandardCharsets.UTF_8);
            // 输出参数
            out.append(param);
            out.flush();
            out.close();

            // 获得响应状态
            int responseCode = httpClient.getResponseCode();
            if(HttpURLConnection.HTTP_OK == responseCode) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpClient.getInputStream(), "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != httpClient) {
                httpClient.disconnect();
            }
        }

        return result.toString();
    }

}
