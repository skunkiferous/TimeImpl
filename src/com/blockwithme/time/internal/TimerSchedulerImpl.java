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

import java.util.Timer;

import com.blockwithme.time.Scheduler;

/**
 * TimerSchedulerImpl implements a Scheduler using a Timer.
 *
 * This implementation assumes, that despite a possible bad system time,
 * the java.util.Timer class can still handle relative delays, rather then
 * absolute date-times, correctly.
 *
 * @author monster
 */
public class TimerSchedulerImpl<T> extends AbstractSchedulerImpl<T> {

    /** Implements the TimerTask wanted by the Timer. */
    private static final class MyTimerTask<T> extends java.util.TimerTask {

        /** The executor running the task. */
        private final Scheduler.Executor<T> executor;

        /** The task to run. */
        private final T task;

        /** Defines a MyTimerTask. */
        public MyTimerTask(final Scheduler.Executor<T> executor, final T task) {
            if (executor == null) {
                throw new NullPointerException("executor");
            }
            if (task == null) {
                throw new NullPointerException("task");
            }
            this.executor = executor;
            this.task = task;
        }

        @Override
        public void run() {
            executor.run(task);
        }
    }

    /** The Timer, implementing the nScheduler. */
    private final Timer timer;

    /**
     * @param executor
     */
    public TimerSchedulerImpl(final Scheduler.Executor<T> executor) {
        super(executor);
        timer = new Timer(true);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Scheduler#cancel()
     */
    @Override
    public void cancel() {
        timer.cancel();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Scheduler#purge()
     */
    @Override
    public int purge() {
        return timer.purge();
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.internal.SchedulerImpl#schedule2(java.lang.Object, long)
     */
    @Override
    protected void schedule2(final T task, final long delayMS) {
        timer.schedule(new MyTimerTask<T>(executor, task), delayMS);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.internal.SchedulerImpl#schedule2(java.lang.Object, long, long)
     */
    @Override
    protected void schedule2(final T task, final long delayMS,
            final long periodMS) {
        timer.schedule(new MyTimerTask<T>(executor, task), delayMS, periodMS);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.internal.SchedulerImpl#scheduleAtFixedRate2(java.lang.Object, long, long)
     */
    @Override
    protected void scheduleAtFixedRate2(final T task, final long delayMS,
            final long periodMS) {
        timer.scheduleAtFixedRate(new MyTimerTask<T>(executor, task), delayMS,
                periodMS);
    }
}
