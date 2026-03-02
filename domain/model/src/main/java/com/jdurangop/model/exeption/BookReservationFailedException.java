package com.jdurangop.model.exeption;

public class BookReservationFailedException extends DomainException {
    public BookReservationFailedException(String message) {
        super(message);
    }
}
