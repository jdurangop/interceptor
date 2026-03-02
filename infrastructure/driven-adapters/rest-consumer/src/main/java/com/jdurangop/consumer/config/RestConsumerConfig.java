package com.jdurangop.consumer.config;

import com.jdurangop.consumer.interceptor.AuthInterceptor;
import com.jdurangop.consumer.interceptor.ErrorMapperInterceptor;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Configuration
public class RestConsumerConfig {
    private final String url;
    private final int timeoutMillis;

    public RestConsumerConfig(String url,
                              int timeoutMillis) {
        this.url = url;
        this.timeoutMillis = timeoutMillis;
    }

    @Bean
    public WebClient getWebClient(WebClient.Builder builder,
                                  ErrorMapperInterceptor errorMapperInterceptor,
                                  AuthInterceptor authInterceptor) {
        return builder
            .baseUrl(url)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(getClientHttpConnector())
            .filter(errorMapperInterceptor)
            .filter(authInterceptor)
            .build();
    }

    private ClientHttpConnector getClientHttpConnector() {
        return new ReactorClientHttpConnector(HttpClient.create()
            .compress(true)
            .keepAlive(true)
            .option(CONNECT_TIMEOUT_MILLIS, timeoutMillis)
            .doOnConnected(connection -> {
                connection.addHandlerLast(new ReadTimeoutHandler(timeoutMillis, MILLISECONDS));
                connection.addHandlerLast(new WriteTimeoutHandler(timeoutMillis, MILLISECONDS));
            }));
    }

}
