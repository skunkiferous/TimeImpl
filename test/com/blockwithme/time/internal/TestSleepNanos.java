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

import java.util.Random;

/**
 * @author monster
 *
 */
public class TestSleepNanos {

    /**
     * @param args
     */
    public static void main(final String[] args) {
        final Random rnd = new Random();
        long sleepDiff = 0;
        for (int i = 0; i < 100; i++) {
            final long sleep = 1000000000L / 60;
            final long adjustedSleep = sleep;// - 900000;
            final long millis = adjustedSleep / 1000000L;
            final int nanos = (int) (adjustedSleep % 1000000L);
            final long before = System.nanoTime();
            try {
                Thread.sleep(millis, nanos);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            final long realSleep = System.nanoTime() - before;
            sleepDiff += (sleep - realSleep);
        }
        System.out.println("AVG DIFF: " + (sleepDiff / 100));
    }

}
