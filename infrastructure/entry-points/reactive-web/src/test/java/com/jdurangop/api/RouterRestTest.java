package com.jdurangop.api;

import com.jdurangop.api.handler.BookHandler;
import com.jdurangop.api.handler.UserHandler;
import com.jdurangop.api.interceptor.HeaderInsertionInterceptor;
import com.jdurangop.api.interceptor.HeaderValidationInterceptor;
import com.jdurangop.api.interceptor.LoggingInterceptor;
import com.jdurangop.model.log.Log;
import com.jdurangop.usecase.log.LogUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = {
    RouterRest.class, BookHandler.class, UserHandler.class, LoggingInterceptor.class,
    HeaderValidationInterceptor.class, HeaderInsertionInterceptor.class
})
@WebFluxTest
class RouterRestTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private LogUseCase logUseCase;

    @Test
    void validateHeadersSuccess() {
        doNothing().when(logUseCase).save(any(Log.class));

        /*successful*/
        var requestId1 = "123";
        webTestClient.get()
            .uri("/api/book/1323")
            .header("X-Request-Id", requestId1)
            .exchange()
            .expectStatus().isOk()
            .expectHeader()
            .valueEquals("X-Request-Id", requestId1);

        var requestId2 = "432";
        webTestClient.post()
            .uri("/api/book")
            .header("X-Request-Id", requestId2)
            .exchange()
            .expectStatus().isOk()
            .expectHeader()
            .valueEquals("X-Request-Id", requestId2);

        //successful, no headers
        webTestClient.post()
            .uri("/api/user")
            .exchange()
            .expectStatus().isOk()
            .expectHeader()
            .doesNotExist("X-Request-Id");

        /*failed*/
        webTestClient.get()
            .uri("/api/book/1323")
            .exchange()
            .expectStatus().isBadRequest();

        webTestClient.post()
            .uri("/api/book")
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void saveLogsSuccess() {
        doNothing().when(logUseCase).save(any(Log.class));

        /*successful*/
        webTestClient.get()
            .uri("/api/book/1323")
            .header("X-Request-Id", "123")
            .exchange()
            .expectStatus().isOk();

        webTestClient.post()
            .uri("/api/book")
            .header("X-Request-Id", "432")
            .exchange()
            .expectStatus().isOk();

        webTestClient.post()
            .uri("/api/user")
            .exchange()
            .expectStatus().isOk();

        /*failed*/
        webTestClient.get()
            .uri("/api/user/42121")
            .exchange()
            .expectStatus().isNotFound();

        verify(logUseCase, times(4)).save(any());
    }
}
