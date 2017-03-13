package com.miapc.ipudong.rest;

import com.miapc.ipudong.vo.ResouceVO;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangwei on 2016/10/14.
 */
@RestController
@EnableDiscoveryClient
@RequestMapping("rest")
public class ResourceController {
	@Value("${service.resource.url}")
	private String resoucesUrl;

	@RequestMapping(value = "/resourcetypes", method = RequestMethod.GET)
	@RequiresRoles("TEST")
	public @ResponseBody Map<String, Object> resourceTypes() {
		Map<String, Object> reslut = new HashMap();
		try {
			HttpGet get = new HttpGet(resoucesUrl);
			get.addHeader("Authorization", "reslibu");
			get.addHeader("Content-Type", "application/json; charset=utf-8");
			get.addHeader(
					" User-Agent",
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36");

			CloseableHttpClient cbHttpClient = createSSLInsecureClient();
			HttpResponse response = cbHttpClient.execute(get);// 报异常
			// 获取验证码字节数组
			String result = EntityUtils.toString(response.getEntity());
			reslut.put("success", true);
			reslut.put("data", result);
		} catch (Exception e) {
			e.printStackTrace();
			reslut.put("error", true);
			reslut.put("errormsg", e.getLocalizedMessage());
		}

		return reslut;
	}

	public static CloseableHttpClient createSSLInsecureClient() {
		try {
			// 创建安全套接字对象
			SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(
					null, new TrustStrategy() {
						// 信任所有
						public boolean isTrusted(X509Certificate[] chain,
								String authType) throws CertificateException {
							return true;
						}
					}).build();
			// 获取分层tls/ssl连接
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					sslContext);
			return HttpClients.custom().setSSLSocketFactory(sslsf).build();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		return HttpClients.createDefault();
	}

}
