package cn.heqichao.httpclient;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Created by heqichao on 2019-7-7.
 */
public class HttpClient4 {
    public static String url ="https://www.baidu.com/";
    public static String keyStore ="D:/java/IdeaProjects/test/src/main/resources/certs/my.keystore";
    public static String keyPassword ="123456";
    private static HttpHost httpHost =new HttpHost("proxIp",8080);
    public static void main(String[] args) {
       // test1();
        test2();
    }

    private static void test1(){
        CloseableHttpClient httpClient = null;
        try {
            // 加载自定义的keystore
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(new File(keyStore), keyPassword.toCharArray()).build();
            // 默认的域名校验类为DefaultHostnameVerifier，比对服务器证书的AlternativeName和CN两个属性。
            // 如果服务器证书这两者不合法而我们又必须让其校验通过，则可以自己实现HostnameVerifier。
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    // 我们可以重写域名校验逻辑
                    return true;
                }
            });
            // 一个httpClient对象对于https仅会选用一个SSLConnectionSocketFactory
            // 至少在4.5.3和4.5.4中，如果给HttpClient对象设置ConnectionManager，我们必须在PoolingHttpClientConnectionManager的构造方法中传入Registry，
            // 并将https对应的工厂设置为我们自己的SSLConnectionSocketFactory对象，因为在DefaultHttpClientConnectionOperator.connect()中，逻辑是从这里找SSLConnectionSocketFactory的。
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslConnectionSocketFactory)
                    .build());
            connectionManager.setMaxTotal(20);
            connectionManager.setDefaultMaxPerRoute(20);

            httpClient = HttpClients.custom()
                    // 不在connectionManager中注册，仅在这里设置SSLConnectionSocketFactory是无效的，详见build()内部逻辑，在connectionManager不为null时，不会使用里的SSLConnectionSocketFactory
                    .setSSLSocketFactory(sslConnectionSocketFactory)
                    .setConnectionManager(connectionManager)
                    .build();
            HttpGet httpGet = new HttpGet(url);
            System.out.println("Executing request " + httpGet.getRequestLine());
           // CloseableHttpResponse response = httpClient.execute(httpHost,httpGet);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            System.out.println("----------------------------------------");
            System.out.println(response.getStatusLine());
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != httpClient)
                {
                    httpClient.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void test2(){
        CloseableHttpClient closeableHttpClient =createSSLClientDefault();
        HttpGet httpGet =new HttpGet(url);
        try {
           //HttpResponse response= closeableHttpClient.execute(httpHost,httpGet);
            HttpResponse response= closeableHttpClient.execute(httpGet);
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建一个SSL信任所有证书的httpClient对象
     *
     * @return
     */
    public static CloseableHttpClient createSSLClientDefault() {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                // 信任所有
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
            return HttpClients.custom().setSSLSocketFactory(sslsf).build();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return HttpClients.createDefault();
    }

}
