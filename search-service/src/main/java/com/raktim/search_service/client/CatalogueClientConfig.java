package com.raktim.search_service.client;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CatalogueClientConfig {

    /** One RestClient for catalogue at catalogue.client.base-url (CAT-76: a plain URL), with both timeouts set. */
    @Bean
    RestClient catalogueRestClient(RestClient.Builder builder, CatalogueClientProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())      // max wait to open the connection
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());  // max wait for the response
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(new RequestIdPropagation())   // passes X-Request-Id on to catalogue
                .build();
    }
}