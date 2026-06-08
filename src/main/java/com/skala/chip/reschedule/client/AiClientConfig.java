package com.skala.chip.reschedule.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * AI 재조정 에이전트(skala-chip-ai) 호출용 RestClient 설정.
 * base-url 과 타임아웃은 application.yml(ai.*) 로 주입한다.
 */
@Configuration
public class AiClientConfig {

    @Bean
    public RestClient aiRestClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${ai.read-timeout-ms}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
