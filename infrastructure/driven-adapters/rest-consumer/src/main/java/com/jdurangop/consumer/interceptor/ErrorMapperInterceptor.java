package com.jdurangop.consumer.interceptor;

import com.jdurangop.consumer.model.response.error.ErrorDto;
import com.jdurangop.model.exeption.*;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ErrorMapperInterceptor implements ExchangeFilterFunction {
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return next.exchange(request)
            .onErrorMap(this::mapToDomainException)
            .flatMap(response -> {
                if (response.statusCode().isError()) {
                    return response.bodyToMono(ErrorDto.class)
                        .flatMap(err -> Mono.error(mapToDomainException(err)));
                }

                return Mono.just(response);
            });
    }

    public DomainException mapToDomainException(ErrorDto errorDto) {
        var message = errorDto.getMessage();

        return switch (errorDto.getCode()) {
            case "404_01" -> new BookNotFoundException(message);
            case "409_01" -> new BookAlreadyReservedException(message);
            case "409_02" -> new BookUnavailableException(message);
            case "500_01" -> new BookReservationFailedException(message);
            default ->
                new DomainException("Unhandled error code: " + errorDto.getCode() + ", message: " + message);
        };
    }

    public DomainException mapToDomainException(Throwable throwable) {
        if (throwable instanceof WebClientRequestException webError
            && webError.getCause() instanceof ReadTimeoutException err) {
            return new TimeOutException(err.getMessage());
        }

        return new DomainException("Unexpected error: " + throwable.getMessage());
    }
}
