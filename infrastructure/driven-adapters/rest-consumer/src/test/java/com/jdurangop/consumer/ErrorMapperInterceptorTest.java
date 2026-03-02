package com.jdurangop.consumer;

import com.jdurangop.consumer.config.RestConsumerConfig;
import com.jdurangop.consumer.gateways.TokenGenerator;
import com.jdurangop.consumer.interceptor.AdvancedAuthInterceptor;
import com.jdurangop.consumer.interceptor.ErrorMapperInterceptor;
import com.jdurangop.consumer.model.response.error.ErrorDto;
import com.jdurangop.model.exeption.BookNotFoundException;
import com.jdurangop.model.exeption.DomainException;
import com.jdurangop.model.exeption.TimeOutException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ErrorMapperInterceptorTest {
    private static RestConsumer restConsumer;
    private static MockWebServer mockBackEnd;
    private static ErrorMapperInterceptor errorMapperInterceptor;

    private static final int TIMEOUT_MS = 500;

    @BeforeEach
    void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();

        var authInterceptor = new AdvancedAuthInterceptor(mock(TokenGenerator.class));
        errorMapperInterceptor = spy(new ErrorMapperInterceptor());
        var config = new RestConsumerConfig(mockBackEnd.url("/").toString(), TIMEOUT_MS);
        var webClient = config.getWebClient(WebClient.builder(), errorMapperInterceptor, authInterceptor);
        restConsumer = new RestConsumer(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void mapErrorBookNotFound() {
        mockBackEnd.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.NOT_FOUND.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"code\": \"404_01\", \"message\": \"Book not found\"}"));

        StepVerifier.create(restConsumer.findById("123"))
            .expectErrorSatisfies(error -> assertInstanceOf(BookNotFoundException.class, error))
            .verify();

        verify(errorMapperInterceptor, times(1)).mapToDomainException(any(ErrorDto.class));
    }

    @Test
    void mapErrorTimeOut() {
        mockBackEnd.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setHeadersDelay(TIMEOUT_MS * 3, MILLISECONDS)
            .setBody("{\"id\":\"123\",\"title\":\"Test Book\",\"author\":\"Author Name\",\"available\":true}"));

        StepVerifier.create(restConsumer.findById("123"))
            .expectErrorSatisfies(error -> assertInstanceOf(TimeOutException.class, error))
            .verify();

        verify(errorMapperInterceptor, times(1)).mapToDomainException(any(Throwable.class));
    }

    @Test
    void mapUnhandledErrorCode() {
        mockBackEnd.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.BAD_REQUEST.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"code\": \"400_01\", \"message\": \"Bad request\"}"));

        StepVerifier.create(restConsumer.findById("123"))
            .expectErrorSatisfies(error -> assertInstanceOf(DomainException.class, error))
            .verify();

        verify(errorMapperInterceptor, times(1)).mapToDomainException(any(ErrorDto.class));
    }
}
