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
package im.xz.cn.runtime;

import im.xz.cn.logging.logApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


public final class ServiceSupervisor {

    private static final logApi log = logApi.getLogger(ServiceSupervisor.class);

    private final Map<ServiceId, ManagedHost> hosts = new LinkedHashMap<>();
    private final Map<ServiceId, ServiceState> states = new EnumMap<>(ServiceId.class);
    private final Map<ServiceId, String> failures = new EnumMap<>(ServiceId.class);

    public synchronized void register(ManagedHost host) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(host.id(), "host.id");
        if (hosts.containsKey(host.id())) {
            throw new IllegalStateException("Duplicate host: " + host.id());
        }
        hosts.put(host.id(), host);
        states.put(host.id(), ServiceState.DISABLED);
    }

    public synchronized void startAll() {
        for (ServiceId id : new ArrayList<>(hosts.keySet())) {
            start(id);
        }
    }

    public synchronized void start(ServiceId id) {
        ManagedHost host = hosts.get(id);
        if (host == null) {
            states.put(id, ServiceState.DISABLED);
            return;
        }
        if (stateOf(id) == ServiceState.READY) {
            return;
        }
        failures.remove(id);
        states.put(id, ServiceState.STARTING);
        try {
            host.start();
            states.put(id, ServiceState.READY);
            log.info("[Supervisor] {} -> READY", id.code());
        } catch (Throwable error) {
            stateFailed(id, error);
        }
    }

    public synchronized void stopAll() {
        List<ServiceId> order = new ArrayList<>(hosts.keySet());
        Collections.reverse(order);
        for (ServiceId id : order) {
            stop(id);
        }
    }

    public synchronized void stop(ServiceId id) {
        ManagedHost host = hosts.get(id);
        if (host == null) {
            return;
        }
        ServiceState current = stateOf(id);
        if (current == ServiceState.STOPPED || current == ServiceState.DISABLED) {
            return;
        }
        states.put(id, ServiceState.STOPPING);
        try {
            host.stop();
            log.info("[Supervisor] {} -> STOPPED", id.code());
        } catch (Throwable error) {
            failures.put(id, "stop failed: " + describe(error));
            log.error("[Supervisor] {} stop failed: {}", id.code(), describe(error), error);
        } finally {
            states.put(id, ServiceState.STOPPED);
        }
    }

    public synchronized ServiceState stateOf(ServiceId id) {
        return states.getOrDefault(id, ServiceState.DISABLED);
    }

    public synchronized boolean isReady(ServiceId id) {
        return stateOf(id) == ServiceState.READY || stateOf(id) == ServiceState.DEGRADED;
    }

    public synchronized boolean anyReady() {
        return states.values().stream().anyMatch(s -> s == ServiceState.READY || s == ServiceState.DEGRADED);
    }

    public synchronized Optional<String> failureOf(ServiceId id) {
        return Optional.ofNullable(failures.get(id));
    }

    public synchronized List<ServiceStatus> statuses() {
        List<ServiceStatus> result = new ArrayList<>(hosts.size());
        for (ServiceId id : hosts.keySet()) {
            result.add(new ServiceStatus(id, stateOf(id), failures.get(id)));
        }
        return List.copyOf(result);
    }

    
    public synchronized String summary() {
        StringBuilder builder = new StringBuilder();
        for (ServiceId id : hosts.keySet()) {
            if (!builder.isEmpty()) {
                builder.append(',');
            }
            builder.append(id.code()).append('=').append(stateOf(id));
        }
        return builder.toString();
    }

    private void stateFailed(ServiceId id, Throwable error) {
        String description = describe(error);
        states.put(id, ServiceState.FAILED);
        failures.put(id, description);
        log.error("[Supervisor] {} -> FAILED: {}", id.code(), description, error);
    }

    private static String describe(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
