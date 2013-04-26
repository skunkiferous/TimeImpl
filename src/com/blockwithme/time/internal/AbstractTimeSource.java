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

import com.blockwithme.time.ClockService;
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

    /** The last produces tick, if any. */
    protected volatile Time lastTick;

    /** Are we paused? */
    protected volatile boolean paused;

    /** Ratio to the parent time source ticks. */
    protected volatile int parentRatio = 1;

    /** The current tick count. */
    protected final AtomicLong ticks = new AtomicLong();

    /** The TimeListener list */
    protected final CopyOnWriteArrayList<Task<TimeListener>> listeners = new CopyOnWriteArrayList<>();

    /** The time at which this time source was created. */
    protected final long startTime;

    /** Crates a AbstractTimeSource. */
    protected AbstractTimeSource(final long theStartTime) {
        startTime = theStartTime;
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
    public int parentRatio() {
        return parentRatio;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#setParentRatio(int)
     */
    @Override
    public void setParentRatio(final int theParentRatio) {
        if (parentRatio == 0) {
            throw new IllegalArgumentException("parentRatio cannot be 0");
        }
        parentRatio = theParentRatio;
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

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if (!paused) {
            final Time lastTickObj = lastTick;
            final long lastTick = (lastTickObj == null) ? 0 : lastTickObj.ticks;
            final int ratio = parentRatio;
            final long ticks = ((ratio > 0) ? 1 : -1) + lastTick;
            if (ticks % ratio == 0) {
                final ClockService clockService = clockService();
                final long now = clockService.currentTimeNanos();
                final Time tickObj = new Time(now, lastTick, ticks, startTime);
                this.lastTick = tickObj;
                for (final Task<TimeListener> t : listeners) {
                    t.task().onTimeChange(tickObj);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.TimeSource#createTimeSource()
     */
    @Override
    public TimeSource createTimeSource() {
        return new DerivedTimeSource(this);
    }
}
