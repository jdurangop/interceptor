package com.jdurangop.api.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static java.util.Objects.isNull;

@Component
public class HeaderValidationInterceptor implements HandlerFilterFunction<ServerResponse, ServerResponse> {
    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        if (isNull(request.headers().firstHeader("X-Request-Id"))) {
            return ServerResponse.badRequest().build();
        }

        return next.handle(request);
    }
}
