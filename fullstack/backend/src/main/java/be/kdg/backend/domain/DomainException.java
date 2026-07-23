package be.kdg.backend.domain;

/**
 * Base class for domain-specific exceptions. Lives in domain (no Infra imports).
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) { super(message); }
    public DomainException(String message, Throwable cause) { super(message, cause); }
}