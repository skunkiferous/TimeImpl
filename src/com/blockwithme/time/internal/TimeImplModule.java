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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blockwithme.time.ClockService;
import com.blockwithme.time.ClockSynchronizer;
import com.blockwithme.time.CoreScheduler;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * Guice module for TimeImpl.
 *
 * @author monster
 */
public class TimeImplModule extends AbstractModule {

    /** Logger. */
    private static final Logger LOG = LoggerFactory
            .getLogger(TimeImplModule.class);

    @Override
    protected void configure() {
        final Multibinder<ClockSynchronizer> mb = Multibinder.newSetBinder(
                binder(), ClockSynchronizer.class);
        mb.addBinding().to(HTTPClockSynchronizer.class);
        mb.addBinding().to(NTPClockSynchronizer.class);
        bind(Long.class).annotatedWith(Names.named("tickDurationNanos"))
                .toInstance(1000000000L / ClockServiceImpl.TICKS_PER_SECONDS);
        bind(CoreScheduler.class).to(TimerCoreScheduler.class);
        bind(Boolean.class).annotatedWith(Names.named("setTimezoneToUTC"))
                .toInstance(true);
        bind(ClockService.class).to(ClockServiceImpl.class);

        LOG.info("TimeImplModule initialized");
    }
}
