package com.github.spring.mq.pulsar.domain;

/**
 * Listener type enumeration
 * 
 * <p>Defines the different types of message listeners supported by the Pulsar starter.
 * Each type has different characteristics and use cases:
 * <ul>
 *   <li>LOOP: Polling-based message consumption in a loop</li>
 *   <li>EVENT: Event-driven message consumption using callbacks</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public enum ListenerType {
    /**
     * Loop-based listening
     * Uses polling mechanism to continuously check for new messages
     */
    LOOP,

    /**
     * Event-driven listening
     * Uses callback mechanism to handle messages as they arrive
     */
    EVENT
}
