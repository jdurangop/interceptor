package com.jdurangop.consumer.interceptor;

import com.jdurangop.consumer.gateways.TokenGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Component
@Slf4j
public class AdvancedAuthInterceptor extends AuthInterceptor {
    private final TokenGenerator tokenGenerator;
    private final AtomicReference<Mono<String>> tokenRefreshRef;
    private final AtomicReference<String> tokenRef;

    public AdvancedAuthInterceptor(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
        this.tokenRefreshRef = new AtomicReference<>();
        this.tokenRef = new AtomicReference<>("Bearer expired-token");
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        var tokenMono = nonNull(tokenRef.get())
            ? Mono.just((tokenRef.get()))
            : refreshToken();

        return tokenMono
            .flatMap(token -> authorizeAndExchange(request, next, token))
            .flatMap(response -> {
                if (response.statusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
                    return refreshToken()
                        .flatMap(token -> authorizeAndExchange(request, next, token));
                }

                return Mono.just(response);
            });
    }

    private Mono<ClientResponse> authorizeAndExchange(ClientRequest request, ExchangeFunction next, String token) {
        var newRequest = ClientRequest.from(request)
            .header(HttpHeaders.AUTHORIZATION, token)
            .build();

        return next.exchange(newRequest);
    }

    private Mono<String> refreshToken() {
        return tokenRefreshRef.updateAndGet(ongoingRefresh -> {
            if (isNull(ongoingRefresh)) {
                tokenRef.set(null);

                return Mono.defer(tokenGenerator::getNew)
                    .cache()
                    .doOnNext(tokenRef::set)
                    .doFinally(signal -> tokenRefreshRef.set(null));
            }

            return ongoingRefresh;
        });
    }
}
