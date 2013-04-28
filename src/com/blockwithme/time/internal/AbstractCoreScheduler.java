/*
 * Copyright (C) 2013 Sebastien Diot.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blockwithme.time.internal;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blockwithme.time.CoreScheduler;
import com.blockwithme.time.Scheduler.Handler;
import com.blockwithme.time.Task;

/**
 * AbstractCoreScheduler implements just the ticker part of the CoreScheduler.
 * The ticker thread is started lazily, on first access.
 *
 * @author monster
 */
public abstract class AbstractCoreScheduler implements CoreScheduler {

    /** Logger. */
    private static final Logger LOG = LoggerFactory
            .getLogger(AbstractCoreScheduler.class);

    /** A task, to "ticker tasks". */
    private static final class TickerTask implements Task<Runnable> {

        /** Have we been closed? */
        private volatile boolean closed;

        /** The Runnable to call. */
        private final Runnable task;

        /** The error handler. */
        private final Handler errorHandler;

        /** The duration of a clock tick in nanoseconds. */
        private final long tickDurationNanos;

        /** Defines a TickerTask. */
        public TickerTask(final Runnable task, final Handler errorHandler,
                final long tickDurationNanos) {
            if (task == null) {
                throw new NullPointerException("task");
            }
            if (errorHandler == null) {
                throw new NullPointerException("errorHandler");
            }
            this.task = task;
            this.errorHandler = errorHandler;
            this.tickDurationNanos = tickDurationNanos;
        }

        /** toString() */
        @Override
        public String toString() {
            return "TickerTask(closed=" + closed + ",task=" + task
                    + ",errorHandler=" + errorHandler + ")";
        }

        @Override
        public void close() throws Exception {
            closed = true;
        }

        @Override
        public Runnable task() {
            return task;
        }

        public boolean run() {
            if (closed) {
                return true;
            }
            final long start = System.nanoTime();
            try {
                task.run();
            } catch (final Throwable t) {
                errorHandler.onError(task, t);
            } finally {
                final long duration = System.nanoTime() - start;
                if (duration > tickDurationNanos) {
                    LOG.error("Task " + task + " took longer then one tick: "
                            + duration / 1000000.0 + " ms");
                } else if (duration > 1000000L) {
                    LOG.warn("Task " + task + " took longer then 1ms: "
                            + duration / 1000000.0 + " ms");
                }
            }
            return false;
        }
    }

    /** The Ticker thread. */
    private static final class TickerThread extends Thread {

        /** Loops while working out sleep overhead. */
        private static final long LOOPS = 100;

        /** Counter required to guarantee unique thread names. */
        private static final AtomicInteger TICKER_THREAD_COUNTER = new AtomicInteger();

        /** The TickerTask list */
        public final CopyOnWriteArrayList<TickerTask> tickers;

        /** The duration of a clock tick in nanoseconds. */
        private final long tickDurationNanos;

        /** Should we stop? */
        public volatile boolean stop;

        /** Creates a Ticker thread. */
        public TickerThread(final CopyOnWriteArrayList<TickerTask> theTickers,
                final long theTickDurationNanos) {
            super("TickerThread#" + TICKER_THREAD_COUNTER.incrementAndGet());
            tickers = theTickers;
            tickDurationNanos = theTickDurationNanos;
        }

        @Override
        public void run() {
            final long start = System.nanoTime();
            long cycle = 0;
            while (!stop) {
                final long cycleStart = System.nanoTime();
                try {
                    final Iterator<TickerTask> iter = tickers.iterator();
                    while (iter.hasNext()) {
                        final TickerTask task = iter.next();
                        if (task.run()) {
                            tickers.remove(task);
                        }
                    }
                } finally {
                    final long duration = System.nanoTime() - cycleStart;
                    if (duration > tickDurationNanos) {
                        LOG.error("Cycle took longer then one tick: "
                                + duration / 1000000.0 + " ms");
                    }
                }
                cycle++;
                final long nextCycle = start + cycle * tickDurationNanos;
                try {
                    AbstractClockServiceImpl.sleepNanosStatic(nextCycle
                            - System.nanoTime());
                } catch (final InterruptedException e) {
                    LOG.error("Got interrupted. Terminating");
                    stop = true;
                }
            }
        }
    }

    /** The duration of a clock tick in nanoseconds. */
    private final long tickDurationNanos;

    /** Should we stop? */
    private volatile boolean stopped;

    /** Ticker thread created? */
    private final AtomicBoolean tickerThreadCreated = new AtomicBoolean();

    /** The TickerTask list */
    private final CopyOnWriteArrayList<TickerTask> tickers = new CopyOnWriteArrayList<TickerTask>();

    /** The optional TickerThread */
    private volatile TickerThread tickerThread;

    /** Creatres a AbstractCoreScheduler. */
    protected AbstractCoreScheduler(final long theTickDurationNanos) {
        tickDurationNanos = theTickDurationNanos;
    }

    /** toString() */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Scheduler#close()
     */
    @Override
    public void close() {
        stopped = true;
        LOG.info(this + " closed.");
        if (!tickerThreadCreated.compareAndSet(false, true)) {
            final TickerThread thread = tickerThread;
            tickerThread = null;
            if (thread != null) {
                LOG.info("Stopped " + thread);
                thread.stop = true;
            }
        }
    }

    @Override
    public Task<Runnable> scheduleTicker(final Runnable task,
            final Handler errorHandler) {
        if (tickerThreadCreated.compareAndSet(false, true)) {
            tickerThread = new TickerThread(tickers, tickDurationNanos);
            LOG.info("Started " + tickerThread);
            tickerThread.setDaemon(true);
            tickerThread.setPriority(Thread.MAX_PRIORITY);
            tickerThread.start();
        }
        if (stopped) {
            throw new IllegalStateException("Stopped!");
        }
        final TickerTask result = new TickerTask(task, errorHandler,
                tickDurationNanos);
        tickers.add(result);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled ticker " + task + " with error handler "
                    + errorHandler);
        }
        return result;
    }
}
