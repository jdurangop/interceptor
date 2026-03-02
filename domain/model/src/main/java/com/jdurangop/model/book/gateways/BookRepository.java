package com.jdurangop.model.book.gateways;

import com.jdurangop.model.book.Book;
import reactor.core.publisher.Mono;

public interface BookRepository {
    Mono<Book> findById(String id);
    Mono<String> save(Book book);
}
