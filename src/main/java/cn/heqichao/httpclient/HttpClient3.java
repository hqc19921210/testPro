package cn.heqichao.httpclient;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.util.HttpURLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by heqichao on 2019-7-7.
 */
public class HttpClient3 {
  //  public static String url ="https://www.baidu.com/";
    public static String url ="https://www.12306.cn";
    public static void main(String[] args) {
        //test1();
        //test2();
        test3();
    }

    private static void test1(){
        HttpClient httpClient = new HttpClient(); //创建客户端

        GetMethod getMethod = new GetMethod(url);
        try {
            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {  //执行成功的标示状态
                System.err.println("Method failed: "
                        + getMethod.getStatusLine());
            }
            // 读取内容
            byte[] responseBody = getMethod.getResponseBody();

           // 处理内容
            String html = new String(responseBody);
            System.out.println(html);
        } catch (Exception e) {
           e.printStackTrace();
        }finally{  //无论成功与否都要释放连接
            getMethod.releaseConnection();
        }
    }

    public static void test2(){
        /**
         * 执行一个http/https get请求，返回请求响应的文本数据
         *
         * @param url           请求的URL地址
         * @param queryString   请求的查询参数,可以为null
         * @param charset       字符集
         * @param pretty        是否美化
         * @return              返回请求响应的文本数据
         */
            StringBuffer response = new StringBuffer();
            HttpClient client = new HttpClient();
            //if(url.startsWith("https")){
                //https请求
                Protocol myhttps = new Protocol("https", new MySSLProtocolSocketFactory(), 443);
                Protocol.registerProtocol("https", myhttps);
         //   }
            HttpMethod method = new GetMethod(url);
            try {
                client.executeMethod(method);
                if (method.getStatusCode() == HttpStatus.SC_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream(), "UTF-8"));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    System.out.println(response.toString());
                }
            }  catch (Exception e) {
                e.printStackTrace();
            } finally {
                method.releaseConnection();
            }
            //return response.toString();

    }


    public static void test3(){
        try {
            System.out.println(proxyHttpRequest(url , "GET", null, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * HTTP请求
     *
     * @param address
     *            地址
     * @param method
     *            方法
     * @param headerParameters
     *            头信息
     * @param body
     *            请求内容
     * @return
     * @throws Exception
     */
    public static String proxyHttpRequest(String address, String method,
                                          Map<String, String> headerParameters, String body) throws Exception {
        String result = null;
        HttpsURLConnection httpConnection = null;

        try {
            httpConnection = createConnection(address, method,
                    headerParameters, body);

            String encoding = "UTF-8";
            if (httpConnection.getContentType() != null
                    && httpConnection.getContentType().indexOf("charset=") >= 0) {
                encoding = httpConnection.getContentType()
                        .substring(
                                httpConnection.getContentType().indexOf(
                                        "charset=") + 8);
            }
            result = inputStream2String(httpConnection.getInputStream(),
                    encoding);
            // logger.info("HTTPproxy response: {},{}", address,
            // result.toString());

        } catch (Exception e) {
            // logger.info("HTTPproxy error: {}", e.getMessage());
            throw e;
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
        return result;
    }

    /**
     * 创建HTTP连接
     *
     * @param url
     *            地址
     * @param method
     *            方法
     * @param headerParameters
     *            头信息
     * @param body
     *            请求内容
     * @return
     * @throws Exception
     */
    private static HttpsURLConnection createConnection(String url,
                                                      String method, Map<String, String> headerParameters, String body)
            throws Exception {
        URL Url = new URL(url);
        trustAllHttpsCertificates();
       // HttpURLConnection httpConnection = (HttpURLConnection) Url.openConnection();
        HttpsURLConnection httpConnection = (HttpsURLConnection) Url.openConnection();
        // 设置请求时间
        //httpConnection.setConnectTimeout(TIMEOUT);
        // 设置 header
        if (headerParameters != null) {
            Iterator<String> iteratorHeader = headerParameters.keySet().iterator();
            while (iteratorHeader.hasNext()) {
                String key = iteratorHeader.next();
                httpConnection.setRequestProperty(key,
                        headerParameters.get(key));
            }
        }
        httpConnection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded;charset=" + "UTF-8");

        // 设置请求方法
        httpConnection.setRequestMethod(method);
        httpConnection.setDoOutput(true);
        httpConnection.setDoInput(true);
        // 写query数据流
        if (!(body == null || body.trim().equals(""))) {
            OutputStream writer = httpConnection.getOutputStream();
            try {
                writer.write(body.getBytes("UTF-8"));
            } finally {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            }
        }

        // 请求结果
        int responseCode = httpConnection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception(responseCode
                    + ":"
                    + inputStream2String(httpConnection.getErrorStream(),
                    "UTF-8"));
        }

        return httpConnection;
    }

    /**
     * 设置 https 请求
     * @throws Exception
     */
    private static void trustAllHttpsCertificates() throws Exception {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String str, SSLSession session) {
                return true;
            }
        });
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager tm = new miTM();
        trustAllCerts[0] = tm;
        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext
                .getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc
                .getSocketFactory());
    }

    private static String inputStream2String(InputStream input, String encoding)
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input,
                encoding));
        StringBuilder result = new StringBuilder();
        String temp = null;
        while ((temp = reader.readLine()) != null) {
            result.append(temp);
        }

        return result.toString();

    }
}

//设置 https 请求证书
 class miTM implements javax.net.ssl.TrustManager,javax.net.ssl.X509TrustManager {

    @Override
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
    }

    public boolean isServerTrusted(
            java.security.cert.X509Certificate[] certs) {
        return true;
    }

    public boolean isClientTrusted(
            java.security.cert.X509Certificate[] certs) {
        return true;
    }

    @Override
    public void checkServerTrusted(
            java.security.cert.X509Certificate[] certs, String authType)
            throws java.security.cert.CertificateException {
        return;
    }

    @Override
    public void checkClientTrusted(
            java.security.cert.X509Certificate[] certs, String authType)
            throws java.security.cert.CertificateException {
        return;
    }


}
