/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miapc.ipudong;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The type Application.
 *
 * @author Spencer Gibb, Kenny Bastani
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableTransactionManagement
@Slf4j
@ComponentScan("com.miapc.ipudong")
public class Application   {

    /**
     * The Logger.
     */
    final Logger logger = LoggerFactory.getLogger(Application.class);

    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    @Value("${spring.application.name}")
    private String appName;

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(true).run(args);
    }

    @Bean
    public RestTemplate getRestTemplate() {
        SSLContext sslcontext = null;
        Set<KeyManager> keymanagers = new LinkedHashSet<>();
        Set<TrustManager> trustmanagers = new LinkedHashSet<>();
        try {
            trustmanagers.add(new HttpsTrustManager());
            KeyManager[] km = keymanagers.toArray(new KeyManager[keymanagers.size()]);
            TrustManager[] tm = trustmanagers.toArray(new TrustManager[trustmanagers.size()]);
            sslcontext = SSLContexts.custom().build();
            sslcontext.init(km, tm, new SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext,
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setSSLSocketFactory(factory);
        // 重试次数，默认是3次，没有开启
        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(2, true));
        // 保持长连接配置，需要在头添加Keep-Alive
        httpClientBuilder.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.16 Safari/537.36"));
        headers.add(new BasicHeader("Accept-Encoding", "gzip,deflate"));
        headers.add(new BasicHeader("Accept-Language", "zh-CN"));
        headers.add(new BasicHeader("Connection", "Keep-Alive"));
        headers.add(new BasicHeader("Authorization", "reslibu"));
        httpClientBuilder.setDefaultHeaders(headers);
        CloseableHttpClient httpClient = httpClientBuilder.build();
        if (httpClient != null) {
            // httpClient连接配置，底层是配置RequestConfig
            HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
            // 连接超时
            clientHttpRequestFactory.setConnectTimeout(60 * 1000);
            // 数据读取超时时间，即SocketTimeout
            clientHttpRequestFactory.setReadTimeout(5 * 60 * 1000);
            // 连接不够用的等待时间，不宜过长，必须设置，比如连接不够用时，时间过长将是灾难性的
            clientHttpRequestFactory.setConnectionRequestTimeout(5000);
            // 缓冲请求数据，默认值是true。通过POST或者PUT大量发送数据时，建议将此属性更改为false，以免耗尽内存。
            // clientHttpRequestFactory.setBufferRequestBody(false);
            // 添加内容转换器
            List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
            messageConverters.add(new StringHttpMessageConverter(Charset.forName("UTF-8")));
            messageConverters.add(new MappingJackson2HttpMessageConverter());
            messageConverters.add(new FormHttpMessageConverter());
            messageConverters.add(new MappingJackson2XmlHttpMessageConverter());

            RestTemplate restTemplate = new RestTemplate(messageConverters);
            restTemplate.setRequestFactory(clientHttpRequestFactory);
            restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
            return restTemplate;
        } else {
            return null;
        }

    }

    public static class HttpsTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

    }
}
