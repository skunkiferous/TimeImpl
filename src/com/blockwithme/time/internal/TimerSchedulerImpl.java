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

/**
 * TimerSchedulerImpl implements a Scheduler using a Timer.
 *
 * This implementation assumes, that despite a possible bad system time,
 * the java.util.Timer class can still handle relative delays, rather then
 * absolute date-times, correctly.
 *
 * @author monster
 */
public class TimerSchedulerImpl extends AbstractSchedulerImpl {

    /** Implements the TimerTask wanted by the Timer. */
    private static final class MyTimerTask extends java.util.TimerTask {

        /** The task to run. */
        private final Runnable task;

        /** The error handler. */
        private final Handler errorHandler;

        /** Defines a MyTimerTask. */
        public MyTimerTask(final Runnable task, final Handler errorHandler) {
            if (task == null) {
                throw new NullPointerException("task");
            }
            if (errorHandler == null) {
                throw new NullPointerException("errorHandler");
            }
            this.task = task;
            this.errorHandler = errorHandler;
        }

        @Override
        public void run() {
            try {
                task.run();
            } catch (final Throwable t) {
                errorHandler.onError(task, t);
            }
        }
    }

    /** The Timer, implementing the nScheduler. */
    private final Timer timer;

    /**
     * @param executor
     */
    public TimerSchedulerImpl(final Handler theErrorHandler) {
        super(theErrorHandler);
        timer = new Timer(true);
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.Scheduler#close()
     */
    @Override
    public void close() {
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
    public void scheduleNS(final Runnable task, final long delayNS) {
        timer.schedule(new MyTimerTask(task, errorHandler), roundToMS(delayNS));
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.internal.SchedulerImpl#scheduleAtFixedPeriodNS(java.lang.Object, long, long)
     */
    @Override
    public void scheduleAtFixedPeriodNS(final Runnable task,
            final long delayNS, final long periodNS) {
        timer.schedule(new MyTimerTask(task, errorHandler), roundToMS(delayNS),
                roundToMS(periodNS));
    }

    /* (non-Javadoc)
     * @see com.blockwithme.time.internal.SchedulerImpl#scheduleAtFixedRateNS(java.lang.Object, long, long)
     */
    @Override
    public void scheduleAtFixedRateNS(final Runnable task, final long delayNS,
            final long periodNS) {
        timer.scheduleAtFixedRate(new MyTimerTask(task, errorHandler),
                roundToMS(delayNS), roundToMS(periodNS));
    }
}
