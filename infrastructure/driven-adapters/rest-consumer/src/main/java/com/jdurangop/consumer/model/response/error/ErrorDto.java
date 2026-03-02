package com.jdurangop.consumer.model.response.error;

import lombok.Data;

@Data
public class ErrorDto {
    private String code;
    private String message;
}
