package com.jdurangop.model.exeption;

public class BookUnavailableException extends DomainException {
    public BookUnavailableException(String message) {
        super(message);
    }
}
