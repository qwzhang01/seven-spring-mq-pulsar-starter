package com.github.spring.mq.pulsar.tracing;

import com.github.spring.mq.pulsar.annotation.ConsumerExceptionHandler;
import com.github.spring.mq.pulsar.annotation.ConsumerExceptionResponse;
import com.github.spring.mq.pulsar.domain.ConsumerExceptionResponseAction;
import com.github.spring.mq.pulsar.exception.JacksonException;
import com.github.spring.mq.pulsar.exception.PulsarConsumerLatterException;

/**
 * 默认异常处理器
 *
 * @author avinzhang
 */
public class ConsumeDefaultExceptionHandler {

    @ConsumerExceptionHandler({UnsupportedOperationException.class, JacksonException.class})
    @ConsumerExceptionResponse(ConsumerExceptionResponseAction.ACK)
    public void unsupportedOperationException(Exception exception) {

    }

    @ConsumerExceptionHandler(PulsarConsumerLatterException.class)
    @ConsumerExceptionResponse(ConsumerExceptionResponseAction.RECONSUME_LATER)
    public void pulsarConsumerLatterException(PulsarConsumerLatterException exception) {

    }

    @ConsumerExceptionHandler(Exception.class)
    @ConsumerExceptionResponse(ConsumerExceptionResponseAction.NACK)
    public void exception(Exception exception) {

    }
}