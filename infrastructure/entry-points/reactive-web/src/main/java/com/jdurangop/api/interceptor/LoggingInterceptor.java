package com.jdurangop.api.interceptor;

import com.jdurangop.model.log.Log;
import com.jdurangop.usecase.log.LogUseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class LoggingInterceptor implements WebFilter {
    private final LogUseCase logUseCase;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
            .doFinally(signal -> {
                var request = exchange.getRequest();
                var response = exchange.getResponse();

                var log = Log.builder()
                    .datetime(LocalDateTime.now())
                    .method(request.getMethod().name())
                    .path(request.getPath().value())
                    .status(response.getStatusCode().value())
                    .build();

                logUseCase.save(log);
            });
    }
}
