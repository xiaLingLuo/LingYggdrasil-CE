/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package im.xz.cn.plugin.runtime;

import im.xz.cn.plugin.spi.PluginScheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public final class PluginTaskScheduler implements PluginScheduler, AutoCloseable {

    private final ScheduledExecutorService executor;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public PluginTaskScheduler(String pluginId) {
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "plugin-" + pluginId);
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public Cancellable runAsync(Runnable task) {
        return wrap(executor.submit(guard(task)));
    }

    @Override
    public Cancellable schedule(Runnable task, long delay, TimeUnit unit) {
        return wrap(executor.schedule(guard(task), delay, unit));
    }

    @Override
    public Cancellable scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return wrap(executor.scheduleAtFixedRate(guard(task), initialDelay, period, unit));
    }

    private Runnable guard(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task is required");
        }
        return () -> {
            if (closed.get()) {
                return;
            }
            task.run();
        };
    }

    private Cancellable wrap(java.util.concurrent.Future<?> future) {
        return () -> future.cancel(false);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            executor.shutdownNow();
        }
    }
}
