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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blockwithme.time.Clock;
import com.blockwithme.time.ClockService;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * The TimeImpl Bundle Activator.
 *
 * @author monster
 */
public class Activator implements BundleActivator {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    /**
     * Initializes the ClockService in Clock.
     */
    private static void start() {
        final Injector injector = Guice.createInjector(new TimeImplModule());
        Clock.setClockService(injector.getInstance(ClockService.class));
        LOG.info("TimeImpl Bundle started at " + Clock.date());
    }

    /** Starts the game. */
    public static void main(final String[] args) {
        start();
    }

    /** {@inheritDoc} */
    @Override
    public void start(final BundleContext context) throws Exception {
        start();
    }

    /** {@inheritDoc} */
    @Override
    public void stop(final BundleContext context) throws Exception {
        LOG.info("TimeImpl Bundle stoped at " + Clock.date());
        Clock.close();
    }
}