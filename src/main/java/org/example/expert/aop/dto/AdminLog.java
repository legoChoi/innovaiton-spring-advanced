package org.example.expert.aop.dto;

import lombok.Getter;

import java.time.LocalDateTime;

public record AdminLog(
        String id,
        String url,
        String method,
        LocalDateTime timestamp
) {

    public AdminLog(String id, String url, String method) {
        this(id, url, method, LocalDateTime.now());
    }
}
