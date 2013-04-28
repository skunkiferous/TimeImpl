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

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import com.blockwithme.time.Task;
import com.blockwithme.time.Time;
import com.blockwithme.time.TimeListener;
import com.blockwithme.time.TimeSource;

/**
 * Abstract time source implementation.
 *
 * @author monster
 */
public abstract class AbstractTimeSource implements TimeSource {

    /** A task to the listeners. */
    private static class MyTask implements Task<TimeListener> {

        /** The TimeListener list */
        private final CopyOnWriteArrayList<Task<TimeListener>> listeners;

        /** The listener. */
        private final TimeListener listener;

        /** Creates my task. */
        public MyTask(
                final CopyOnWriteArrayList<Task<TimeListener>> theListeners,
                final TimeListener theListener) {
            listeners = theListeners;
            listener = theListener;
        }

        /* (non-Javadoc)
         * @see java.lang.AutoCloseable#close()
         */
        @Override
        public void close() throws Exception {
            listeners.remove(this);
        }

        /* (non-Javadoc)
         * @see com.blockwithme.time.Task#task()
         */
        @Override
        public TimeListener task() {
            return listener;
        }
    }

    /** The time source name. */
    private final String name;

    /** The last produces tick, if any. */
    private volatile Time lastTick;

    /** Are we paused? */
    private volatile boolean paused;

    /** Divider to the parent time source ticks. */
    protected volatile int clockDivider = 1;

    /** The current tick count. */
    private final AtomicLong ticks = new AtomicLong();

    /** The TimeListener list */
    private final CopyOnWriteArrayList<Task<TimeListener>> listeners = new CopyOnWriteArrayList<>();

    /** The time at which this time source was created. */
    private final long startTime;

    /** The total time spent in paused state, since this time source was created. */
    private final AtomicLong pausedTime = new AtomicLong();

    /** The number of ticks skipped since the last non-skipped tick. */
    private long skippedTicks;

    /** The number of parent ticks since start. */
    private long parentTicks;

    /** Crates a AbstractTimeSource. */
    protected AbstractTimeSource(final long theStartTime, final String theName) {
        startTime = theStartTime;
        if (theName == null) {
            throw new IllegalArgumentException("theName is null");
        }
        if (theName.isEmpty()) {
            throw new IllegalArgumentException("theName is empty");
        }
        name = theName;
    }

    /** toString() */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(name=" + name + ")";
    }

    /** Returns the name. */
    @Override
    public String name() {
        return name;
    }

    /** Returns the time at which this time source was created. */
    @Override
    public long startTime() {
        return startTime;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#lastTick()
     */
    @Override
    public Time lastTick() {
        return lastTick;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#offsetTicks(long)
     */
    @Override
    public long offsetTicks(final long offset) {
        return ticks.getAndAdd(offset);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#setTicks(long)
     */
    @Override
    public void setTicks(final long theTicks) {
        ticks.set(theTicks);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#ticks()
     */
    @Override
    public long ticks() {
        return ticks.get();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#parentRatio()
     */
    @Override
    public int clockDivider() {
        return clockDivider;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#setClockDivider(int)
     */
    @Override
    public void setClockDivider(final int theClockDivider) {
        if (theClockDivider == 0) {
            throw new IllegalArgumentException("theClockDivider cannot be 0");
        }
        clockDivider = theClockDivider;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#pause()
     */
    @Override
    public void pause() {
        paused = true;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#unpause()
     */
    @Override
    public void unpause() {
        paused = false;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#registerListener(com.blockwithme.time.TimeListener)
     */
    @Override
    public Task<TimeListener> registerListener(final TimeListener listener) {
        final MyTask result = new MyTask(listeners,
                Objects.requireNonNull(listener));
        listeners.add(result);
        return result;
    }

    /**
     * Called when the parent time source produces a tick.
     * The tick step (usually 1) is passed as parameter.
     */
    protected final void tick(final long step, final long currentTimeNanos) {
        parentTicks += step;
        final Time lastTickObj = lastTick;
        final long lastTicks = (lastTickObj == null) ? 0 : lastTickObj.ticks;
        final int ratio = clockDivider;
        if (parentTicks % ratio == 0) {
            if (paused) {
                skippedTicks++;
            } else {
                final long ticks = ((ratio > 0) ? 1 : -1) + lastTicks;
                final long pausedTicks = skippedTicks;
                skippedTicks = 0;
                // TODO Compute real value of pausedTime ...
                final long pausedTime = tickPeriode() * pausedTicks;
                this.pausedTime.addAndGet(pausedTime);
                final TimeSource timeSource = this;
                if (lastTickObj != null) {
                    // Prevent infinite list of Time instances.
                    lastTickObj.lastTick = null;
                }
                final Time tickObj = new Time(timeSource, currentTimeNanos,
                        lastTickObj, ticks, pausedTicks, pausedTime);
                this.lastTick = tickObj;
                for (final Task<TimeListener> t : listeners) {
                    t.task().onTimeChange(tickObj);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#pausedTime()
     */
    @Override
    public long pausedTime() {
        return pausedTime.get();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#ticksPerSecond()
     */
    @Override
    public float ticksPerSecond() {
        return ((float) Time.SECOND_NS) / tickPeriode();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#createTimeSource()
     */
    @Override
    public TimeSource newTimeSource(final String theName,
            final boolean pausedAtStart, final boolean inheritTickCount) {
        return new DerivedTimeSource(this, theName, pausedAtStart,
                inheritTickCount);
    }
}
