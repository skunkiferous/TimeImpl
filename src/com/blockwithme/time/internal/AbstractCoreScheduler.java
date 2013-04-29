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
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.CoreScheduler;
import com.blockwithme.time.Scheduler.Handler;
import com.blockwithme.time.Task;
import com.blockwithme.time.Time;

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

        /** The ClockService */
        private final ClockService clockService;

        /** The duration of a clock tick in nanoseconds. */
        private final long tickDurationNanos;

        /** Should we stop? */
        public volatile boolean stop;

        /** Creates a Ticker thread. */
        public TickerThread(final CopyOnWriteArrayList<TickerTask> theTickers,
                final long theTickDurationNanos,
                final ClockService theClockService) {
            super("TickerThread#" + TICKER_THREAD_COUNTER.incrementAndGet());
            tickers = theTickers;
            tickDurationNanos = theTickDurationNanos;
            clockService = Objects.requireNonNull(theClockService,
                    "theClockService");
        }

        @Override
        public void run() {
            LOG.info("Tick Duration (ns): " + tickDurationNanos);
            final long start = clockService.currentTimeNanos();
            long cycle = 0;
            while (!stop) {
                final long cycleStart = clockService.currentTimeNanos();
                final long end;
                try {
                    final Iterator<TickerTask> iter = tickers.iterator();
                    while (iter.hasNext()) {
                        final TickerTask task = iter.next();
                        if (task.run()) {
                            tickers.remove(task);
                        }
                    }
                } finally {
                    end = clockService.currentTimeNanos();
                    final long duration = end - cycleStart;
                    if (duration > tickDurationNanos) {
                        LOG.error("Cycle took longer then one tick: "
                                + duration / 1000000.0 + " ms");
                    }
                }
                cycle++;
                // TODO Deal with jumped-over cycles; replace Runnable with some
                // interface taking a int as number of elapsed cycles?
                final long nextCycle = start + cycle * tickDurationNanos;
                try {
                    AbstractClockServiceImpl.sleepNanosStatic(nextCycle - end);
                } catch (final InterruptedException e) {
                    LOG.error("Got interrupted. Terminating");
                    stop = true;
                }
            }
        }
    }

    /** The duration of a clock tick in nanoseconds. */
    private final long tickDurationNanos;

    /** The number of clock ticks per second */
    private final int ticksPerSecond;

    /** Should we stop? */
    private volatile boolean stopped;

    /** Ticker thread created? */
    private final AtomicBoolean tickerThreadCreated = new AtomicBoolean();

    /** The TickerTask list */
    private final CopyOnWriteArrayList<TickerTask> tickers = new CopyOnWriteArrayList<TickerTask>();

    /** The optional TickerThread */
    private volatile TickerThread tickerThread;

    /** The ClockService. Not set at creation time, due to dependency cycles. */
    private final AtomicReference<ClockService> clockService = new AtomicReference<>();

    /** Creates a AbstractCoreScheduler. */
    protected AbstractCoreScheduler(final int theTicksPerSecond) {
        ticksPerSecond = theTicksPerSecond;
        tickDurationNanos = Time.SECOND_NS / ticksPerSecond;
    }

    /** toString() */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /** Sets the ClockService. Can only be called once. */
    @Override
    public void setClockService(final ClockService theClockService) {
        if (!clockService.compareAndSet(null, theClockService)) {
            throw new IllegalStateException("ClockService already set!");
        }
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
            tickerThread = new TickerThread(tickers, tickDurationNanos,
                    clockService.get());
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

    /* (non-Javadoc)
     * @see com.blockwithme.time.ClockService#ticksPerSecond()
     */
    @Override
    public int ticksPerSecond() {
        return ticksPerSecond;
    }
}
