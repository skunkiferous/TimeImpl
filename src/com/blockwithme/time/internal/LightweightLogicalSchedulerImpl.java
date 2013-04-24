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
import java.util.concurrent.atomic.AtomicLong;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.CoreScheduler;
import com.blockwithme.time.LogicalScheduler;
import com.blockwithme.time.LogicalTimeListener;
import com.blockwithme.time.Task;

/**
 * A LightweightSchedulerImpl that also support logical application time.
 *
 * TODO The current implementation will be really slow if we have many listeners.
 *
 * @author monster
 */
public class LightweightLogicalSchedulerImpl extends LightweightSchedulerImpl
        implements LogicalScheduler {

    private static final class LogicalTask implements Task<LogicalTimeListener> {

        /** Are we closed? */
        private volatile boolean closed;

        /** If period is 0, then we run only once. */
        private final long period;

        /** First activation time. */
        private final long start;

        /** The real task. */
        private final LogicalTimeListener task;

        public LogicalTask(final LogicalTimeListener theTask,
                final long thePeriod, final long theStart) {
            task = Objects.requireNonNull(theTask);
            period = thePeriod;
            start = theStart;
        }

        @Override
        public void close() {
            closed = true;
        }

        /* (non-Javadoc)
         * @see com.blockwithme.time.Task#task()
         */
        @Override
        public LogicalTimeListener task() {
            return task;
        }

        /** Returns true, if the task should be called again. */
        public boolean onTimeChange(final long before, final long after) {
            if (!closed && (after >= start)) {
                if (period == 0) {
                    closed = true;
                    task.onTimeChange(before, after);
                } else if ((after - start) % period == 0) {
                    task.onTimeChange(before, after);
                }
            }
            return closed;
        }

    };

    /** The listeners. */
    private final CopyOnWriteArrayList<LogicalTask> listeners = new CopyOnWriteArrayList<>();

    /** The current logical time. */
    private final AtomicLong logicalTime = new AtomicLong();

    /** The incrementer task. */
    private final Runnable incrementer = new Runnable() {
        @Override
        public void run() {
            incrementLogicalTime();
        }
    };

    /** The decrementer task. */
    private final Runnable decrementer = new Runnable() {
        @Override
        public void run() {
            decrementLogicalTime();
        }
    };

    /**
     * @param theCoreScheduler
     * @param theErrorHandler
     * @param theClockService
     */
    public LightweightLogicalSchedulerImpl(
            final CoreScheduler theCoreScheduler,
            final Handler theErrorHandler, final ClockService theClockService,
            final long cycleDuration, final boolean fixedRate) {
        super(theCoreScheduler, theErrorHandler, theClockService);
        if (cycleDuration == 0) {
            throw new IllegalArgumentException("cycleDuration cannot be 0");
        }
        final Runnable task;
        final long cycle;
        if (cycleDuration > 0) {
            task = incrementer;
            cycle = cycleDuration;
        } else {
            task = decrementer;
            cycle = -cycleDuration;
        }
        if (fixedRate) {
            scheduleAtFixedRate(task, cycle, cycle);
        } else {
            scheduleAtFixedPeriod(task, cycle, cycle);
        }
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.LogicalTimeSource#logicalTime()
     */
    @Override
    public long logicalTime() {
        return logicalTime.get();
    }

    /** Sets the logical time. */
    @Override
    public void setLogicalTime(final long theLogicalTime) {
        logicalTimeChanged(logicalTime.getAndSet(theLogicalTime),
                theLogicalTime);
    }

    /** Increments the logical time. */
    @Override
    public long incrementLogicalTime() {
        final long theLogicalTime = logicalTime.incrementAndGet();
        logicalTimeChanged(theLogicalTime - 1, theLogicalTime);
        return theLogicalTime;
    }

    /** Decrements the logical time. */
    @Override
    public long decrementLogicalTime() {
        final long theLogicalTime = logicalTime.decrementAndGet();
        logicalTimeChanged(theLogicalTime + 1, theLogicalTime);
        return theLogicalTime;
    }

    /** Sets the logical time. */
    private void logicalTimeChanged(final long before, final long after) {
        for (final Iterator<LogicalTask> iter = listeners.iterator(); iter
                .hasNext();) {
            if (iter.next().onTimeChange(before, after)) {
                iter.remove();
            }
        }
    }

    /** Schedules a task to be executed in delay cycles. */
    @Override
    public Task<LogicalTimeListener> schedule(final LogicalTimeListener task,
            final long delay) {
        final LogicalTask result = new LogicalTask(task, 0, logicalTime()
                + delay);
        listeners.add(result);
        return result;
    }

    /** Schedules a task to be executed every period cycles, starting in delay cycles. */
    @Override
    public Task<LogicalTimeListener> scheduleAtFixedRate(
            final LogicalTimeListener task, final long delay, final long period) {
        final LogicalTask result = new LogicalTask(task, period, logicalTime()
                + delay);
        listeners.add(result);
        return result;
    }
}
