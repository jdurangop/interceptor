package com.jdurangop.api.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class HeaderInsertionInterceptor implements HandlerFilterFunction<ServerResponse, ServerResponse> {
    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        return next.handle(request)
            .flatMap(response -> ServerResponse.from(response)
                .header("X-Request-Id", Objects.requireNonNull(request.headers().firstHeader("X-Request-Id")))
                .build());
    }
}
