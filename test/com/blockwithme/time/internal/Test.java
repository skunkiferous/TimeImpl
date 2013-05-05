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

import org.threeten.bp.Instant;

/**
 * @author monster
 *
 */
public class Test {
    public static void main(final String[] args) {
        final Instant minValue = Instant.ofEpochSecond(0, Long.MIN_VALUE);
        final Instant maxValue = Instant.ofEpochSecond(0, Long.MAX_VALUE);
        System.out.println("minValue: " + minValue);
        System.out.println("maxValue: " + maxValue);
    }
}
