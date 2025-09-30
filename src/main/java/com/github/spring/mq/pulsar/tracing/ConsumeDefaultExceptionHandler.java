/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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