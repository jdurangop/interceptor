package com.jdurangop.consumer;

import com.jdurangop.consumer.config.RestConsumerConfig;
import com.jdurangop.consumer.gateways.TokenGenerator;
import com.jdurangop.consumer.interceptor.AdvancedAuthInterceptor;
import com.jdurangop.consumer.interceptor.AuthInterceptor;
import com.jdurangop.consumer.interceptor.ErrorMapperInterceptor;
import com.jdurangop.consumer.interceptor.SimpleAuthInterceptor;
import com.jdurangop.model.book.Book;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {
    private static RestConsumer restConsumer;
    private static MockWebServer mockBackEnd;
    @Mock
    private static TokenGenerator tokenGenerator;
    private static AuthInterceptor authInterceptor;
    private static ErrorMapperInterceptor errorInterceptor;
    private static RestConsumerConfig restConsumerConfig;

    private static final String VALID_TOKEN = "Bearer valid-token";

    @BeforeEach
    void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();

        mockBackEnd.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NonNull RecordedRequest request) {
                var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

                // check if the token is valid, return 401 if not
                if (!VALID_TOKEN.equals(authHeader)) {
                    log.error("Unauthorized request with token: {}", authHeader);
                    return new MockResponse()
                        .setResponseCode(HttpStatus.UNAUTHORIZED.value());
                }

                log.info("Authorized request");
                return new MockResponse()
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setResponseCode(HttpStatus.OK.value())
                    .setBody("{\"id\":\"123\",\"title\":\"Test Book\",\"author\":\"Author Name\",\"available\":true}");
            }
        });

        errorInterceptor = new ErrorMapperInterceptor();
        restConsumerConfig = new RestConsumerConfig(mockBackEnd.url("/").toString(), 60 * 60 * 1000);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void findByIdSuccess_ShouldIncludeToken() {
        authInterceptor = new SimpleAuthInterceptor(tokenGenerator);
        var webClient = restConsumerConfig.getWebClient(WebClient.builder(), errorInterceptor, authInterceptor);
        restConsumer = new RestConsumer(webClient);

        when(tokenGenerator.getNew())
            .thenReturn(Mono.just(VALID_TOKEN));

        StepVerifier.create(restConsumer.findById("1"))
            .expectNextMatches(Book::isAvailable)
            .verifyComplete();

        verify(tokenGenerator, times(1)).getNew();
        assertEquals(1, mockBackEnd.getRequestCount());
    }

    @Test
    void makeSeveralRequests_ShouldIncludeTokens() {
        authInterceptor = new SimpleAuthInterceptor(tokenGenerator);
        var webClient = restConsumerConfig.getWebClient(WebClient.builder(), errorInterceptor, authInterceptor);
        restConsumer = new RestConsumer(webClient);

        when(tokenGenerator.getNew())
            .thenReturn(Mono.just(VALID_TOKEN));

        var requestMono = Mono.zip(
            restConsumer.findById("1"),
            restConsumer.findById("2"),
            restConsumer.findById("3")
        );

        StepVerifier.create(requestMono)
            .expectNextCount(1)
            .verifyComplete();

        verify(tokenGenerator, times(3)).getNew();
        assertEquals(3, mockBackEnd.getRequestCount());
    }

    @Test
    void makeSeveralRequests_ShouldReuseToken() {
        authInterceptor = new AdvancedAuthInterceptor(tokenGenerator);
        var webClient = restConsumerConfig.getWebClient(WebClient.builder(), errorInterceptor, authInterceptor);
        restConsumer = new RestConsumer(webClient);

        when(tokenGenerator.getNew())
            .thenReturn(Mono.delay(Duration.ofMillis(1000))
                .map(l -> VALID_TOKEN));

        var requests = Mono.zip(
            // in parallel, starts token refresh process, two requests sent to server
            restConsumer.findById("1"),
            // in parallel, starts token refresh process, two requests sent to server
            restConsumer.findById("2"),
            // after 500ms, token still refreshing, should wait for the new token, one request sent to server
            Mono.delay(Duration.ofMillis(500)).flatMap(l -> restConsumer.findById("3")),
            // after 1500ms, token should be refreshed, should use the new token, one request sent to server
            Mono.delay(Duration.ofMillis(1500)).flatMap(l -> restConsumer.findById("4"))
        );

        StepVerifier.create(requests)
            .expectNextCount(1)
            .verifyComplete();

        verify(tokenGenerator, times(1)).getNew();
        assertEquals(6, mockBackEnd.getRequestCount());
    }
}