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
import com.blockwithme.time.Task;
import com.blockwithme.time.Time;
import com.blockwithme.time.implapi.CoreScheduler;
import com.blockwithme.time.implapi.Ticker;

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
    private static final class TickerTask implements Task<Ticker> {

        /** Have we been closed? */
        private volatile boolean closed;

        /** The Runnable to call. */
        private final Ticker task;

        /** The duration of a clock tick in microseconds. */
        private final long tickDurationMicros;

        /** Defines a TickerTask. */
        public TickerTask(final Ticker task, final long tickDurationMicros) {
            if (task == null) {
                throw new NullPointerException("task");
            }
            this.task = task;
            this.tickDurationMicros = tickDurationMicros;
        }

        /** toString() */
        @Override
        public String toString() {
            return "TickerTask(closed=" + closed + ",task=" + task + ")";
        }

        @Override
        public void close() throws Exception {
            closed = true;
        }

        @Override
        public Ticker task() {
            return task;
        }

        public boolean run(final int ticks, final long cycleStart) {
            if (closed) {
                return true;
            }
            final long start = System.nanoTime();
            try {
                return task.onTick(ticks, cycleStart);
            } catch (final Throwable t) {
                LOG.error("Task " + task + " failed", t);
            } finally {
                final long duration = (System.nanoTime() - start)
                        / Time.MICROSECOND_NANOS;
                if (duration > tickDurationMicros) {
                    LOG.error("Task " + task + " took longer then one tick: "
                            + duration / ((double) Time.MILLI_MUS) + " ms");
                } else if (duration > Time.MILLI_MUS) {
                    LOG.warn("Task " + task + " took longer then 1ms: "
                            + duration / ((double) Time.MILLI_MUS) + " ms");
                }
            }
            return false;
        }
    }

    /** The Ticker thread. */
    private static final class TickerThread extends Thread {

        /** Counter required to guarantee unique thread names. */
        private static final AtomicInteger TICKER_THREAD_COUNTER = new AtomicInteger();

        /** The TickerTask list */
        public final CopyOnWriteArrayList<TickerTask> tickers;

        /** The ClockService */
        private final ClockService clockService;

        /** The duration of a clock tick in microseconds. */
        private final long tickDurationMicros;

        /** Should we stop? */
        public volatile boolean stop;

        /** Creates a Ticker thread. */
        public TickerThread(final CopyOnWriteArrayList<TickerTask> theTickers,
                final long theTickDurationMicros,
                final ClockService theClockService) {
            super("TickerThread#" + TICKER_THREAD_COUNTER.incrementAndGet());
            tickers = theTickers;
            tickDurationMicros = theTickDurationMicros;
            clockService = Objects.requireNonNull(theClockService,
                    "theClockService");
        }

        @Override
        public void run() {
            LOG.info("Tick Duration (ms): " + tickDurationMicros);
            final long start = clockService.currentTimeMicros();
            long cycle = 0;
            long prevStart = start;
            while (!stop) {
                final long cycleStart = clockService.currentTimeMicros();
                final long elapsedMicros = (cycleStart - prevStart);
                LOG.debug("Tick Duration (ns): " + elapsedMicros);
                final int elapsedCycles = (int) Math
                        .round(((double) elapsedMicros) / tickDurationMicros);
                final Iterator<TickerTask> iter = tickers.iterator();
                while (iter.hasNext()) {
                    final TickerTask task = iter.next();
                    if (task.run(elapsedCycles, cycleStart)) {
                        tickers.remove(task);
                    }
                }
                final long end = clockService.currentTimeMicros();
                final long duration = end - cycleStart;
                if (duration > tickDurationMicros) {
                    LOG.error("Cycle took longer then one tick: " + duration
                            / ((double) Time.MILLI_MUS) + " ms");
                }
                cycle++;
                final long nextCycle = start + cycle * tickDurationMicros;
                try {
                    AbstractClockServiceImpl.sleepMicrosStatic(nextCycle - end);
                } catch (final InterruptedException e) {
                    LOG.error("Got interrupted. Terminating");
                    stop = true;
                }
                prevStart = cycleStart;
            }
        }
    }

    /** The duration of a clock tick in microseconds. */
    private final long tickDurationMicros;

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
        tickDurationMicros = Math.round((double) Time.SECOND_MUS
                / ticksPerSecond);
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
    public Task<Ticker> scheduleTicker(final Ticker task) {
        if (tickerThreadCreated.compareAndSet(false, true)) {
            tickerThread = new TickerThread(tickers, tickDurationMicros,
                    clockService.get());
            LOG.info("Started " + tickerThread);
            tickerThread.setDaemon(true);
            tickerThread.setPriority(Thread.MAX_PRIORITY);
            tickerThread.start();
        }
        if (stopped) {
            throw new IllegalStateException("Stopped!");
        }
        final TickerTask result = new TickerTask(task, tickDurationMicros);
        tickers.add(result);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled ticker " + task);
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
