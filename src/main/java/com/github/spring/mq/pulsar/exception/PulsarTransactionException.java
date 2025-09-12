package com.github.spring.mq.pulsar.exception;

import org.springframework.transaction.TransactionException;

/**
 * Pulsar 事务异常
 *
 * @author avinzhang
 */
public class PulsarTransactionException extends TransactionException {
    public PulsarTransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
