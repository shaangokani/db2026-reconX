package com.dbtraining.reconx.audit;

import org.springframework.context.ApplicationEventPublisher;

public class AuditEventPublisher {

    private final ApplicationEventPublisher publisher;
    private final AuditProperties properties;

    // This is the constructor the AutoConfiguration is looking for!
    public AuditEventPublisher(ApplicationEventPublisher publisher, AuditProperties properties) {
        this.publisher = publisher;
        this.properties = properties;
    }

    // (You will likely add your actual event publishing methods here next)
}