package com.github.spring.mq.pulsar.exception;

import org.springframework.transaction.TransactionException;

/**
 * Pulsar transaction exception
 * 
 * <p>This exception is thrown when there are errors during Pulsar transaction
 * operations, such as transaction commit failures, rollback issues,
 * or transaction timeout problems. It extends Spring's TransactionException
 * to integrate with Spring's transaction management infrastructure.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarTransactionException extends TransactionException {
    public PulsarTransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
