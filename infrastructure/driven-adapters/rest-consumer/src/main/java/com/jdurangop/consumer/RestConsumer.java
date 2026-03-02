package com.jdurangop.consumer;

import com.jdurangop.consumer.model.response.book.BookDto;
import com.jdurangop.model.book.Book;
import com.jdurangop.model.book.gateways.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RestConsumer implements BookRepository {
    private final WebClient webClient;

    @Override
    public Mono<Book> findById(String id) {
        return webClient.get()
            .uri("/books/{id}", id)
            .retrieve()
            .bodyToMono(Book.class);
    }

    @Override
    public Mono<String> save(Book book) {
        return webClient.post()
            .uri("/books")
            .bodyValue(book)
            .retrieve()
            .bodyToMono(BookDto.class)
            .map(BookDto::getId);
    }
}
