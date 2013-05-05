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
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blockwithme.time.Scheduler;
import com.blockwithme.time.Task;
import com.blockwithme.time.Ticker;
import com.blockwithme.time.Time;
import com.blockwithme.time.TimeListener;
import com.blockwithme.time.Timeline;
import com.blockwithme.time.TimelineBuilder;
import com.blockwithme.time._Scheduler;

/**
 * TimelineImpl2 is an implementation of Timeline.
 *
 * @author monster
 */
public abstract class TimelineImpl implements Timeline, Ticker {

    /** A task to the listeners. */
    private static class MyTask<E> implements Task<E> {

        /** The TimeListener list */
        private final CopyOnWriteArrayList<?> listeners;

        /** The listener. */
        private final E listener;

        /** Creates my task. */
        public MyTask(final CopyOnWriteArrayList<?> theListeners,
                final E theListener) {
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
        public E task() {
            return listener;
        }
    }

    /** Contains most of the mutable data of the timeline. */
    private static final class Data implements Cloneable {
        /** Last *core* tick time. */
        public long lastCoreTickNanos;

        /** The time at which this timeline was created. */
        public long startTime;

        /** The running time, in nanoseconds. */
        public long runningElapsedTime;

        /** The pause time, in nanoseconds. */
        public long pausedElapsedTime;

        /**
         * The total ticks spent in paused state, since this timeline was
         * created.
         */
        public long pausedElapsedTicks;

        /**
         * The total ticks spent in running state, since this timeline was
         * created.
         */
        public long runningElapsedTicks;

        /** The last produces tick, if any. */
        public Time lastTick;

        /** The number of core ticks, since creation. */
        public long coreTicks;

        /** Clones the Data. */
        @Override
        public Data clone() {
            try {
                return (Data) super.clone();
            } catch (final CloneNotSupportedException e) {
                throw new IllegalStateException("Impossible!", e);
            }
        }
    }

    /** Logger */
    private static final Logger LOG = LoggerFactory
            .getLogger(TimelineImpl.class);

    /** The timeline name. */
    private final String name;

    /** The offset added, to produce time() */
    private final double timeOffset;

    /** Should the timeline loop, when reaching it's fixed duration? (If any) */
    private final boolean loopWhenReachingEnd;

    /** The local scaling, of the ticks, to form the time. */
    private final double localTickScaling;

    /** Do we have a limited, fixed, duration (in ticks) ? */
    private final long fixedDurationTicks;

    /** The local tick step. */
    private final double localTickStep;

    /** Are we paused? */
    private volatile boolean pausedLocally;

    /** Are we closed? */
    private volatile boolean closed;

    /** The total time spent in paused state, since this timeline was created. */
    private final AtomicReference<Data> data = new AtomicReference<>();

    /** The TimeListener list */
    private final CopyOnWriteArrayList<Task<TimeListener>> listeners = new CopyOnWriteArrayList<>();

    /** The children list */
    private final CopyOnWriteArrayList<Ticker> children = new CopyOnWriteArrayList<>();

    /** Crates a AbstractTimeline. */
    protected TimelineImpl(final String theName, final long theStartTime,
            final double theTimeOffset, final boolean theLoopWhenReachingEnd,
            final double theLocalTickScaling, final long theFixedDurationTicks,
            final double theLocalTickStep) {
        if (theName == null) {
            throw new IllegalArgumentException("theName is null");
        }
        if (theName.isEmpty()) {
            throw new IllegalArgumentException("theName is empty");
        }
        name = theName;
        timeOffset = theTimeOffset;
        loopWhenReachingEnd = theLoopWhenReachingEnd;
        localTickScaling = theLocalTickScaling;
        fixedDurationTicks = theFixedDurationTicks;
        localTickStep = theLocalTickStep;
        final Data d = new Data();
        d.startTime = theStartTime;
        d.lastCoreTickNanos = theStartTime;
        data.set(d);
    }

    /** toString() */
    @Override
    public String toString() {
        final Data d = data.get();
        return getClass().getSimpleName() + "(name=" + name + ",pausedLocally="
                + pausedLocally + ",timeOffset=" + timeOffset + ",tickPeriode="
                + tickPeriod() + ",loopWhenReachingEnd=" + loopWhenReachingEnd
                + ",startTime=" + d.startTime + ",pausedTime="
                + d.pausedElapsedTime + ",localTickScaling=" + localTickScaling
                + ",fixedDurationTicks=" + fixedDurationTicks
                + ",localTickStep=" + localTickStep + ",pausedElapsedTicks="
                + d.pausedElapsedTicks + ",runningElapsedTicks="
                + d.runningElapsedTicks + ",runningElapsedTime="
                + d.runningElapsedTime + ",ticksPerSecond=" + ticksPerSecond()
                + ",globalTickScaling=" + globalTickScaling()
                + ",globalTickStep=" + globalTickStep() + ",time=" + time()
                + ")";
    }

    /** Returns the name. */
    @Override
    public String name() {
        return name;
    }

    /** Returns the time at which this timeline was created. */
    @Override
    public long startTimePoint() {
        return data.get().startTime;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline#lastTick()
     */
    @Override
    public Time lastTick() {
        return data.get().lastTick;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline#pause()
     */
    @Override
    public void pause() {
        pausedLocally = true;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline#unpause()
     */
    @Override
    public void unpause() {
        pausedLocally = false;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline#paused()
     */
    @Override
    public boolean pausedLocally() {
        return pausedLocally;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline#registerListener(com.blockwithme.time.TimeListener)
     */
    @Override
    public Task<TimeListener> registerListener(final TimeListener listener) {
        final MyTask<TimeListener> result = new MyTask<TimeListener>(listeners,
                Objects.requireNonNull(listener));
        listeners.add(result);
        return result;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#registerListener(com.blockwithme.time.Ticker)
     */
    @Override
    public Task<Ticker> registerListener(final Ticker listener) {
        final MyTask<Ticker> result = new MyTask<Ticker>(children,
                Objects.requireNonNull(listener));
        children.add(listener);
        return result;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline#pausedTime()
     */
    @Override
    public long pausedElapsedTime() {
        return data.get().pausedElapsedTime;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline#ticksPerSecond()
     */
    @Override
    public double ticksPerSecond() {
        return ((double) Time.SECOND_NS) / tickPeriod();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#runningElapsedTime()
     */
    @Override
    public long runningElapsedTime() {
        return data.get().runningElapsedTime;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#totalElapsedTime()
     */
    @Override
    public long totalElapsedTime() {
        return pausedElapsedTime() + runningElapsedTime();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#localTickStep()
     */
    @Override
    public double localTickStep() {
        return localTickStep;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#pausedElapsedTicks()
     */
    @Override
    public long pausedElapsedTicks() {
        return data.get().pausedElapsedTicks;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#runningElapsedTicks()
     */
    @Override
    public long runningElapsedTicks() {
        return data.get().runningElapsedTicks;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#totalElapsedTicks()
     */
    @Override
    public long totalElapsedTicks() {
        return pausedElapsedTicks() + runningElapsedTicks();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#fixedDurationTicks()
     */
    @Override
    public long fixedDurationTicks() {
        return fixedDurationTicks;
    }

    /** Computes the progress. */
    private double progress(final long theRunningElapsedTicks) {
        if (fixedDurationTicks == 0) {
            return -1;
        }
        return ((double) theRunningElapsedTicks) / fixedDurationTicks;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#progress()
     */
    @Override
    public double progress() {
        return progress(runningElapsedTicks());
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#loopWhenReachingEnd()
     */
    @Override
    public boolean loopWhenReachingEnd() {
        return loopWhenReachingEnd;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#timeOffset()
     */
    @Override
    public double timeOffset() {
        return timeOffset;
    }

    /** Computes the time. */
    private double time(final long theRunningElapsedTicks) {
        return timeOffset + globalTickScaling() * theRunningElapsedTicks;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#time()
     */
    @Override
    public double time() {
        return time(runningElapsedTicks());
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#localTickScaling()
     */
    @Override
    public double localTickScaling() {
        return localTickScaling;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#startTimePointSec()
     */
    @Override
    public double startTimePointSec() {
        return ((double) startTimePoint()) / Time.SECOND_NS;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#pausedElapsedTimeSec()
     */
    @Override
    public double pausedElapsedTimeSec() {
        return (pausedElapsedTimeSec()) / Time.SECOND_NS;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#runningElapsedTimeSec()
     */
    @Override
    public double runningElapsedTimeSec() {
        return (runningElapsedTimeSec()) / Time.SECOND_NS;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#totalElapsedTimeSec()
     */
    @Override
    public double totalElapsedTimeSec() {
        return (totalElapsedTimeSec()) / Time.SECOND_NS;
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#newChildTimeline(boolean)
     */
    @Override
    public TimelineBuilder newChildTimeline(final boolean cloneState,
            final Scheduler scheduler) {
        return new TimelineBuilderImpl2(this, this, cloneState,
                (_Scheduler) scheduler);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#reset()
     */
    @Override
    public void reset() {
        final Data newData = new Data();
        newData.startTime = newData.lastCoreTickNanos = clockService()
                .currentTimeNanos();
        data.set(newData);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Timeline2#tickPeriod()
     */
    @Override
    public long tickPeriod() {
        return Math
                .round(globalTickStep() * clockService().tickDurationNanos());
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Ticker#onTick(int,long)
     */
    @Override
    public boolean onTick(final int step, final long timeNanos) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(name() + " onTick(" + step + ", " + timeNanos + ")");
        }
        if (closed) {
            for (final Ticker t : children) {
                // Delegating close ...
                try {
                    if (t instanceof AutoCloseable) {
                        ((AutoCloseable) t).close();
                    }
                    // Close is not really performed, until onTick() is called.
                    t.onTick(step, timeNanos);
                } catch (final Exception e) {
                    LOG.error("Error closing " + t, e);
                }
            }
            children.clear();
            for (final Task<TimeListener> t : listeners) {
                // null, to signify closure of the timeline.
                t.task().onTimeChange(null);
            }
            listeners.clear();
            return true;
        }
        final Data d = data.get();
        final Data copy = d.clone();
        copy.lastCoreTickNanos = timeNanos;
        final long elapsedTimeSinceLastCoreTick = timeNanos
                - d.lastCoreTickNanos;
        Time newTime = null;
        if (pausedGlobally()) {
            copy.pausedElapsedTicks += step;
            copy.pausedElapsedTime += elapsedTimeSinceLastCoreTick;
        } else {
            copy.coreTicks += step;
            copy.runningElapsedTime += elapsedTimeSinceLastCoreTick;
            final double globalTickStep = globalTickStep();
            final long ticksNow = ((long) (copy.coreTicks / globalTickStep));
            final long ticksBefore = ((long) ((copy.coreTicks - 1) / globalTickStep));
            if (ticksBefore != ticksNow) {
                // TODO : In the worst case, if step is big, we could have even skipped ticks!
                copy.runningElapsedTicks++;
                final double progress = progress(copy.runningElapsedTicks);
                final double time = time(copy.runningElapsedTicks);
                newTime = new Time(this, copy.coreTicks, timeNanos,
                        copy.runningElapsedTime, progress, time,
                        copy.runningElapsedTicks, d.lastTick);
                if (newTime.lastTick != null) {
                    newTime.lastTick.lastTick = null;
                }
                copy.lastTick = newTime;
            }
        }
        data.set(copy);
        for (final Ticker t : children) {
            t.onTick(step, timeNanos);
        }
        if (newTime != null) {
            for (final Task<TimeListener> t : listeners) {
                t.task().onTimeChange(newTime);
            }
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        closed = true;
    }
}
