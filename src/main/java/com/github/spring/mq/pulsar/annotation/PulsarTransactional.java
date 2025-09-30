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

package com.github.spring.mq.pulsar.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Pulsar transaction annotation
 *
 * <p>This is a composite annotation that combines Spring's @Transactional annotation,
 * specifically designed for Pulsar transaction management. It provides a convenient
 * way to enable transactional behavior for Pulsar message operations.
 *
 * <p>Features:
 * <ul>
 *   <li>Automatic transaction management for Pulsar operations</li>
 *   <li>Integration with Spring's transaction infrastructure</li>
 *   <li>Configurable transaction behavior and isolation levels</li>
 *   <li>Support for rollback conditions and timeout settings</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * @PulsarTransactional
 * public void processMessage(MyMessage message) {
 *     // Transactional message processing
 *     pulsarTemplate.send("output-topic", processedMessage);
 * }
 * }
 * </pre>
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional("pulsarTransactionManager")
public @interface PulsarTransactional {

    /**
     * Transaction manager name
     */
    @AliasFor(annotation = Transactional.class, attribute = "transactionManager")
    String transactionManager() default "pulsarTransactionManager";

    /**
     * Transaction propagation behavior
     */
    @AliasFor(annotation = Transactional.class, attribute = "propagation")
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * Transaction isolation level
     */
    @AliasFor(annotation = Transactional.class, attribute = "isolation")
    Isolation isolation() default Isolation.DEFAULT;

    /**
     * Transaction timeout in seconds
     */
    @AliasFor(annotation = Transactional.class, attribute = "timeout")
    int timeout() default -1;

    /**
     * Transaction timeout string representation
     */
    @AliasFor(annotation = Transactional.class, attribute = "timeoutString")
    String timeoutString() default "";

    /**
     * Whether this is a read-only transaction
     */
    @AliasFor(annotation = Transactional.class, attribute = "readOnly")
    boolean readOnly() default false;

    /**
     * Exception types that will cause transaction rollback
     */
    @AliasFor(annotation = Transactional.class, attribute = "rollbackFor")
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * Exception class names that will cause transaction rollback
     */
    @AliasFor(annotation = Transactional.class, attribute = "rollbackForClassName")
    String[] rollbackForClassName() default {};

    /**
     * Exception types that will NOT cause transaction rollback
     */
    @AliasFor(annotation = Transactional.class, attribute = "noRollbackFor")
    Class<? extends Throwable>[] noRollbackFor() default {};

    /**
     * Exception class names that will NOT cause transaction rollback
     */
    @AliasFor(annotation = Transactional.class, attribute = "noRollbackForClassName")
    String[] noRollbackForClassName() default {};

    /**
     * Transaction labels for transaction identification
     */
    @AliasFor(annotation = Transactional.class, attribute = "label")
    String[] label() default {};
}