package com.jdurangop.consumer.gateways;

import reactor.core.publisher.Mono;

public interface TokenGenerator {
    Mono<String> getNew();
}
