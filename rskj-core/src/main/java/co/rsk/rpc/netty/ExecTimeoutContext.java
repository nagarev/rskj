/*
 * This file is part of RskJ
 * Copyright (C) 2018 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.rpc.netty;

import java.util.HashSet;
import java.util.Set;

public class ExecTimeoutContext implements AutoCloseable {

    private static final ThreadLocal<Set<ExecTimeoutContext>> sExecTimeoutContext = ThreadLocal.withInitial(HashSet::new);

    /**
     * Creates a new context, which at some point has to be closed.
     * <p>
     * <pre>
     *         try (ExecTimeoutContext ignored = ExecTimeoutContext.create(5000)) { // 5 secs timeout
     *             while (true) {
     *                 ExecTimeoutContext.checkIfExpired();
     *
     *                 // some other code
     *             }
     *         } catch (ExecTimeoutContext.TimeoutException e) {
     *             e.printStackTrace();
     *         }
     * </pre>
     *
     * @param timeout time after which this exec context should be considered expired.
     */
    public static ExecTimeoutContext create(long timeout) {
        Set<ExecTimeoutContext> ctxs = sExecTimeoutContext.get();
        ExecTimeoutContext ctx = new ExecTimeoutContext(timeout);
        ctxs.add(new ExecTimeoutContext(timeout));
        sExecTimeoutContext.set(ctxs);

        return ctx;
    }

    public static void checkIfExpired() {
        sExecTimeoutContext.get().forEach(ExecTimeoutContext::checkIfExpired);
    }

    public static void checkIfExpired(ExecTimeoutContext execTimeoutContext) {
        if (execTimeoutContext != null && execTimeoutContext.timeoutInMillis > 0) {
            long currentTimeInMillis = System.currentTimeMillis();

            if (currentTimeInMillis > execTimeoutContext.expirationTimeInMillis) {
                throw new TimeoutException("Execution has expired.");
            }
        }
    }

    private final long expirationTimeInMillis;
    private final long timeoutInMillis;
    private final HashSet<ExecTimeoutContext> execTimeoutContexts = new HashSet<>();

    private ExecTimeoutContext(long timeoutInMillis) {
        this.timeoutInMillis = timeoutInMillis;
        this.expirationTimeInMillis = System.currentTimeMillis() + timeoutInMillis;
    }

    @Override
    public void close() {
        Set<ExecTimeoutContext> ctxs = sExecTimeoutContext.get();
        ctxs.remove(this);

        if (ctxs.isEmpty()) {
            sExecTimeoutContext.remove();
        }
    }

    public void close(ExecTimeoutContext execTimeoutContext) {
        Set<ExecTimeoutContext> ctxs = execTimeoutContext.execTimeoutContexts;
        ctxs.remove(this);

        for (ExecTimeoutContext ctx : ctxs) {
            close(ctx);
        }
    }

    public static class TimeoutException extends RuntimeException {

        public TimeoutException(String message) {
            super(message);
        }
    }
}
