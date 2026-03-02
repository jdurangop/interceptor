package com.jdurangop.consumer.interceptor;

import com.jdurangop.consumer.gateways.TokenGenerator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class SimpleAuthInterceptor extends AuthInterceptor {
    private final TokenGenerator tokenGenerator;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return tokenGenerator.getNew()
            .map(token -> ClientRequest.from(request)
                .header(HttpHeaders.AUTHORIZATION, token)
                .build())
            .flatMap(next::exchange);
    }
}

