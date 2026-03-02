package com.jdurangop.api.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserHandler {
    public Mono<ServerResponse> getFindById(ServerRequest serverRequest) {
        return ServerResponse.notFound().build();
    }

    public Mono<ServerResponse> postSave(ServerRequest serverRequest) {
        return ServerResponse.ok().build();
    }
}
