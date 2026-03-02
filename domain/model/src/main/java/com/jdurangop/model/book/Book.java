package com.jdurangop.model.book;

import lombok.Data;

@Data
public class Book {
    private String id;
    private String title;
    private String author;
    private boolean available;
}
