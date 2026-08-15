package com.startup.platform.auth.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "auth_outbox_events")
public class AuthOutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 128)
    private String aggregateType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    protected AuthOutboxEvent() {
        // Required by JPA.
    }

    public AuthOutboxEvent(
            UUID eventId,
            String aggregateType,
            String payload,
            Status status,
            Integer retryCount) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getPayload() {
        return payload;
    }

    public Status getStatus() {
        return status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public enum Status {
        PENDING,
        PROCESSED,
        FAILED
    }
}