package com.jdurangop.model.exeption;

public class BookNotFoundException extends DomainException {
    public BookNotFoundException(String message) {
        super(message);
    }
}
