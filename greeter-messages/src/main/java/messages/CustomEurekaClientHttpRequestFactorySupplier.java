package messages;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.netflix.eureka.TimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class CustomEurekaClientHttpRequestFactorySupplier implements EurekaClientHttpRequestFactorySupplier {

	private static final Logger log = LoggerFactory.getLogger(CustomEurekaClientHttpRequestFactorySupplier.class);

	private final TimeoutProperties timeoutProperties;

	public CustomEurekaClientHttpRequestFactorySupplier(@Qualifier("eureka.client.restclient.timeout-org.springframework.cloud.netflix.eureka.RestClientTimeoutProperties") TimeoutProperties timeoutProperties) {
		this.timeoutProperties = timeoutProperties;
	}

	@Override
	public ClientHttpRequestFactory get(SSLContext sslContext, @Nullable HostnameVerifier hostnameVerifier) {
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		if (sslContext != null || hostnameVerifier != null || timeoutProperties != null) {
			httpClientBuilder
					.setConnectionManager(buildConnectionManager(sslContext, hostnameVerifier, timeoutProperties));
		}
		if (timeoutProperties != null) {
			httpClientBuilder.setDefaultRequestConfig(buildRequestConfig());
		}

		CloseableHttpClient httpClient = httpClientBuilder.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClient);
		return requestFactory;
	}

	private HttpClientConnectionManager buildConnectionManager(SSLContext sslContext, HostnameVerifier hostnameVerifier,
			TimeoutProperties restTemplateTimeoutProperties) {
		PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder
				.create();
		SSLConnectionSocketFactoryBuilder sslConnectionSocketFactoryBuilder = SSLConnectionSocketFactoryBuilder
				.create();
		if (sslContext != null) {
			sslConnectionSocketFactoryBuilder.setSslContext(sslContext);
		}
		if (hostnameVerifier != null) {
			sslConnectionSocketFactoryBuilder.setHostnameVerifier(hostnameVerifier);
		}
		connectionManagerBuilder.setSSLSocketFactory(sslConnectionSocketFactoryBuilder.build());
		if (restTemplateTimeoutProperties != null) {
			connectionManagerBuilder.setDefaultSocketConfig(SocketConfig.custom()
					.setSoTimeout(Timeout.of(restTemplateTimeoutProperties.getSocketTimeout(), TimeUnit.MILLISECONDS))
					.build());
		}
		return connectionManagerBuilder.build();
	}

	private RequestConfig buildRequestConfig() {
		return RequestConfig.custom()
				.setProtocolUpgradeEnabled(false)
				.setConnectTimeout(Timeout.of(timeoutProperties.getConnectTimeout(), TimeUnit.MILLISECONDS))
				.setConnectionRequestTimeout(
						Timeout.of(timeoutProperties.getConnectRequestTimeout(), TimeUnit.MILLISECONDS))
				.build();
	}

}
