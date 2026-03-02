package com.jdurangop.model.exeption;

public class BookAlreadyReservedException extends DomainException {
    public BookAlreadyReservedException(String message) {
        super(message);
    }
}
