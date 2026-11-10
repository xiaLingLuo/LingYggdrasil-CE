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
package im.xz.cn.notification;

import im.xz.cn.logging.logApi;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public final class NotificationWorker implements AutoCloseable {

    private static final logApi log = logApi.getLogger(NotificationWorker.class);

    public interface Dispatcher {
        void dispatch(NotificationJob job) throws Exception;
    }

    private final NotificationJobRepository jobs;
    private final Dispatcher dispatcher;
    private final int maxAttempts;
    private final Duration pollInterval;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public NotificationWorker(NotificationJobRepository jobs, Dispatcher dispatcher,
                              int maxAttempts, Duration pollInterval) {
        this.jobs = jobs;
        this.dispatcher = dispatcher;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.pollInterval = pollInterval == null ? Duration.ofSeconds(5) : pollInterval;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "notification-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            executor.scheduleWithFixedDelay(this::pollSafely, 0,
                    pollInterval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    
    public int pollOnce() {
        List<NotificationJob> due = jobs.findDue(50);
        int processed = 0;
        for (NotificationJob job : due) {
            processed++;
            try {
                dispatcher.dispatch(job);
                jobs.markSent(job.id());
            } catch (Exception error) {
                NotificationState state = jobs.markFailed(
                        job.id(), maxAttempts, Instant.now().plus(backoff(job.attempts() + 1)));
                log.warn("[notification] dispatch failed id={} type={} state={}: {}",
                        job.id(), job.type(), state, error.getMessage());
            }
        }
        return processed;
    }

    private void pollSafely() {
        try {
            pollOnce();
        } catch (RuntimeException error) {
            log.error("[notification] poll failed: {}", error.getMessage(), error);
        }
    }

    private static Duration backoff(int attempt) {
        long seconds = Math.min(300L, 30L * Math.max(1, attempt));
        return Duration.ofSeconds(seconds);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
