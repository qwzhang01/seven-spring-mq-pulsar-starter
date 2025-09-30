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

package com.github.spring.mq.pulsar.transaction;

import com.github.spring.mq.pulsar.config.PulsarTransactionConfiguration;
import org.apache.pulsar.client.api.transaction.Transaction;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Pulsar transaction utilities
 *
 * <p>This utility class provides convenient methods for working with
 * Pulsar transactions in a Spring-managed environment. It integrates
 * with Spring's transaction management infrastructure to provide
 * seamless transaction support.
 *
 * <p>Features:
 * <ul>
 *   <li>Current transaction retrieval</li>
 *   <li>Transaction status checking</li>
 *   <li>Transaction ID extraction</li>
 *   <li>Integration with Spring TransactionSynchronizationManager</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarTransactionUtils {

    /**
     * Get current thread-bound Pulsar transaction
     *
     * @return Current transaction, or null if no transaction is active
     */
    public static Transaction getCurrentTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return null;
        }

        // Get transaction resource from transaction synchronization manager
        Object resource = TransactionSynchronizationManager.getResourceMap().values()
                .stream()
                .filter(r -> r instanceof PulsarTransactionConfiguration.PulsarTransactionHolder)
                .findFirst()
                .orElse(null);

        if (resource instanceof PulsarTransactionConfiguration.PulsarTransactionHolder) {
            return ((PulsarTransactionConfiguration.PulsarTransactionHolder) resource).getTransaction();
        }

        return null;
    }

    /**
     * Check if Pulsar transaction is currently active
     *
     * @return true if transaction exists, false otherwise
     */
    public static boolean isTransactionActive() {
        return getCurrentTransaction() != null;
    }

    /**
     * Get current transaction ID
     *
     * @return Transaction ID, or null if no transaction is active
     */
    public static String getCurrentTransactionId() {
        Transaction transaction = getCurrentTransaction();
        return transaction != null ? transaction.getTxnID().toString() : null;
    }
}