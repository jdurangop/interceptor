package com.jdurangop.model.log;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
public class Log {
    private LocalDateTime datetime;
    private String method;
    private String path;
    private int status;
    private String statusDescription;
}
