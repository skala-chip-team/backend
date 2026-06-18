package com.skala.chip.reschedule.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

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
                .messageConverters(AiClientConfig::tolerateOctetStreamJson)
                .build();
    }

    /**
     * AI 서비스 일부 엔드포인트(/run 등)가 JSON 본문을 application/octet-stream 으로
     * 응답하는 경우가 있어, Jackson 컨버터가 octet-stream 도 JSON 으로 읽도록 미디어타입을 보강한다.
     * (정상 application/json 응답 처리에는 영향 없음)
     */
    private static void tolerateOctetStreamJson(List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
        for (org.springframework.http.converter.HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jackson) {
                List<MediaType> supported = new ArrayList<>(jackson.getSupportedMediaTypes());
                if (!supported.contains(MediaType.APPLICATION_OCTET_STREAM)) {
                    supported.add(MediaType.APPLICATION_OCTET_STREAM);
                    jackson.setSupportedMediaTypes(supported);
                }
            }
        }
    }
}
