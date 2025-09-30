package com.github.spring.mq.pulsar.tracing;

import com.github.spring.mq.pulsar.annotation.ConsumerExceptionHandler;
import com.github.spring.mq.pulsar.annotation.ConsumerExceptionResponse;
import com.github.spring.mq.pulsar.domain.ConsumerExceptionResponseAction;
import com.github.spring.mq.pulsar.exception.JacksonException;
import com.github.spring.mq.pulsar.exception.PulsarConsumerLatterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default exception handler for message consumption
 *
 * <p>Provides default exception handling strategies for common exceptions
 * that may occur during message consumption. This handler defines how
 * different types of exceptions should be handled and what response
 * actions should be taken.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class ConsumeDefaultExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConsumeDefaultExceptionHandler.class);

    @ConsumerExceptionHandler({UnsupportedOperationException.class, JacksonException.class})
    @ConsumerExceptionResponse(ConsumerExceptionResponseAction.ACK)
    public void unsupportedOperationException(Exception exception) {
        logger.error("Error processing message", exception);
    }

    @ConsumerExceptionHandler(PulsarConsumerLatterException.class)
    @ConsumerExceptionResponse(ConsumerExceptionResponseAction.RECONSUME_LATER)
    public void pulsarConsumerLatterException(PulsarConsumerLatterException exception) {
        logger.error("Error processing message", exception);
    }

    @ConsumerExceptionHandler(Exception.class)
    @ConsumerExceptionResponse(ConsumerExceptionResponseAction.NACK)
    public void exception(Exception exception) {
        logger.error("Error processing message", exception);
    }
}