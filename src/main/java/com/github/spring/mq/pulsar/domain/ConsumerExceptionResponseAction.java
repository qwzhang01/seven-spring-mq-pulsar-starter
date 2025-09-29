package com.github.spring.mq.pulsar.domain;

/**
 * Response action after message consumption exception
 * 
 * <p>Defines how to handle messages when consumption fails:
 * <ul>
 *   <li>ACK: Acknowledge the message (mark as successfully processed)</li>
 *   <li>NACK: Negative acknowledge the message (reject and trigger redelivery)</li>
 *   <li>RECONSUME_LATER: Schedule the message for later consumption</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public enum ConsumerExceptionResponseAction {
    ACK, NACK, RECONSUME_LATER
}