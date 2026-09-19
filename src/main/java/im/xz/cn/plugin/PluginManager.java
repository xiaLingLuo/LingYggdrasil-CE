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
package im.xz.cn.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.config.AppConfig;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.permission.PermissionNode;
import im.xz.cn.permission.PermissionRegistry;
import im.xz.cn.plugin.api.LingPlugin;
import im.xz.cn.plugin.api.PluginApiHandler;
import im.xz.cn.plugin.api.PluginMenu;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.router.InternalRouter;
import io.javalin.router.ParsedEndpoint;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PluginManager {
    private static final PluginSystemLog log = PluginSystemLog.get();
    private static final PluginManager INSTANCE = new PluginManager();
    private static final String STATE_FILE = "plugins-state.json";
    private static final List<HandlerType> WILDCARD_METHODS = List.of(
            HandlerType.GET, HandlerType.POST, HandlerType.PUT, HandlerType.DELETE, HandlerType.PATCH);

    private static final java.util.regex.Pattern API_RANGE_PATTERN =
            java.util.regex.Pattern.compile("^(\\d+(?:\\.\\d+)*)to(\\d+(?:\\.\\d+)*)$");
    private static final List<String> RESERVED_PREFIXES = List.of(
            "/api", "/authserver", "/sessionserver", "/admin",
            "/css", "/js", "/img", "/icons", "/builtin-icons", "/favicon.ico");

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, PluginHolder> plugins = new LinkedHashMap<>();
    private final Map<String, PluginStateEntry> state = new LinkedHashMap<>();
    private final Map<String, RouteEntry> userRoutes = new ConcurrentHashMap<>();
    private final Map<String, RouteEntry> yggdrasilRoutes = new ConcurrentHashMap<>();
    private final Set<String> builtinUserRoutes = ConcurrentHashMap.newKeySet();
    private final Set<String> builtinYggdrasilRoutes = ConcurrentHashMap.newKeySet();

    private File pluginsDir;
    private File stateFile;
    private DatabaseManager databaseManager;
    private boolean bootstrapped;

    private record RouteEntry(String plugin, PluginApiHandler handler) {}

    private PluginManager() {}

    public static PluginManager getInstance() {
        return INSTANCE;
    }

    public File getPluginsDir() {
        return pluginsDir;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public boolean isBootstrapped() {
        return bootstrapped;
    }

    public synchronized void captureBuiltinRoutes(InternalRouter router, boolean userServer) {
        if (router == null) return;
        Set<String> target = userServer ? builtinUserRoutes : builtinYggdrasilRoutes;
        target.clear();
        for (ParsedEndpoint parsed : router.allHttpHandlers()) {
            if (parsed == null || parsed.endpoint == null) continue;
            HandlerType type = parsed.endpoint.method;
            String path = parsed.endpoint.path;
            if (type == null || !type.isHttpMethod() || path == null) continue;
            if (path.indexOf('*') >= 0 || path.indexOf('{') >= 0) continue;
            target.add(routeKey(type, path));
        }
    }

    public synchronized void registerUserRoute(String plugin, String method, String path, PluginApiHandler handler) {
        registerRoute(userRoutes, "user", plugin, method, path, handler);
    }

    public synchronized void registerYggdrasilRoute(String plugin, String method, String path, PluginApiHandler handler) {
        registerRoute(yggdrasilRoutes, "yggdrasil", plugin, method, path, handler);
    }

    private void registerRoute(Map<String, RouteEntry> routes, String serverName,
                               String plugin, String method, String path, PluginApiHandler handler) {
        if (handler == null) return;
        String normalizedPath = normalizeRoutePath(path);
        if (normalizedPath == null) {
            log.warn("[Plugin] {} rejected invalid route path '{}'", plugin, path);
            return;
        }
        if (isReservedPath(normalizedPath)) {
            log.warn("[Plugin] Route {} is reserved by LingYggdrasil, '{}' rejected", normalizedPath, plugin);
            return;
        }
        List<HandlerType> methods = resolveMethods(method);
        if (methods.isEmpty()) {
            log.warn("[Plugin] {} rejected route '{}': unsupported method '{}'", plugin, normalizedPath, method);
            return;
        }

        Set<String> builtinRoutes = (routes == yggdrasilRoutes) ? builtinYggdrasilRoutes : builtinUserRoutes;
        for (HandlerType type : methods) {
            String key = routeKey(type, normalizedPath);
            if (builtinRoutes.contains(key)) {
                log.warn("[Plugin] Route {} {} is owned by a LingYggdrasil built-in route, '{}' rejected",
                        type.name(), normalizedPath, plugin);
                return;
            }
            RouteEntry existing = routes.get(key);
            if (existing != null && !existing.plugin().equalsIgnoreCase(plugin)) {
                log.warn("[Plugin] Route conflict on {} {}: already registered by {}, '{}' rejected",
                        type.name(), normalizedPath, existing.plugin(), plugin);
                return;
            }
        }

        for (HandlerType type : methods) {
            routes.put(routeKey(type, normalizedPath), new RouteEntry(plugin, handler));
        }
        log.info("[Plugin] {} registered {} route {} {}", plugin, serverName, method, normalizedPath);
    }

    public boolean handleUserRequest(Context ctx) {
        return handleRequest(userRoutes, ctx);
    }

    public boolean handleYggdrasilRequest(Context ctx) {
        return handleRequest(yggdrasilRoutes, ctx);
    }

    private boolean handleRequest(Map<String, RouteEntry> routes, Context ctx) {
        if (routes.isEmpty()) return false;
        String path = normalizeRequestPath(ctx.path());
        if (path == null) return false;
        RouteEntry entry = routes.get(routeKey(ctx.method(), path));
        if (entry == null) return false;
        try {
            entry.handler().handle(new PluginRequestImpl(ctx, path), new PluginResponseImpl(ctx));
        } catch (Throwable t) {
            log.error("[Plugin] Route {} {} of {} failed: {}", ctx.method().name(), path, entry.plugin(), t.getMessage());
            try {
                if (!ctx.res().isCommitted()) {
                    ctx.status(500).json(Map.of("success", false, "message", "Plugin error"));
                }
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    private void unregisterPluginRoutes(String plugin) {
        userRoutes.entrySet().removeIf(e -> e.getValue().plugin().equalsIgnoreCase(plugin));
        yggdrasilRoutes.entrySet().removeIf(e -> e.getValue().plugin().equalsIgnoreCase(plugin));
    }

    private static boolean isReservedPath(String path) {
        for (String prefix : RESERVED_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) return true;
        }
        return false;
    }

    private static String normalizeRequestPath(String path) {
        if (path == null) return null;
        String p = path.trim();
        if (p.isEmpty()) return "/";
        if (!p.startsWith("/")) p = "/" + p;
        while (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }

    private static String normalizeRoutePath(String path) {
        if (path == null) return null;
        String p = path.trim();
        if (p.isEmpty()) return null;
        if (!p.startsWith("/")) p = "/" + p;
        while (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        if (p.indexOf('{') >= 0 || p.indexOf('}') >= 0 || p.indexOf('*') >= 0
                || p.indexOf('?') >= 0 || p.indexOf('#') >= 0 || p.indexOf(' ') >= 0
                || p.contains("..")) {
            return null;
        }
        return p;
    }

    private static List<HandlerType> resolveMethods(String method) {
        if (method == null || method.isBlank() || "*".equals(method.trim())) {
            return WILDCARD_METHODS;
        }
        HandlerType type = HandlerType.findOrCreate(method.trim().toUpperCase(Locale.ROOT));
        if (type == null || !type.isHttpMethod()) return List.of();
        return List.of(type);
    }

    private static String routeKey(HandlerType type, String path) {
        return type.name() + " " + path;
    }

    public synchronized void bootstrap(DatabaseManager databaseManager) {
        if (bootstrapped) return;
        this.databaseManager = databaseManager;

        BuiltinPlugin.register();

        File dir = new File(System.getProperty("user.dir"), "plugins");
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("[Plugin] Failed to create plugins directory: {}", dir.getAbsolutePath());
        }
        this.pluginsDir = dir;
        this.stateFile = new File(dir, STATE_FILE);

        state.clear();
        state.putAll(loadState());

        List<PluginDescriptor> descriptors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        File[] jars = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (jars != null) {
            Arrays.sort(jars, Comparator.comparing(File::getName));
            for (File jar : jars) {
                PluginYaml.ParseResult result = PluginYaml.parse(jar.toPath());
                if (!result.ok()) {
                    log.warn("[Plugin] Ignored '{}': {}", jar.getName(), result.error());
                    continue;
                }
                PluginDescriptor descriptor = result.descriptor();
                if (!seen.add(descriptor.name().toLowerCase(Locale.ROOT))) {
                    log.warn("[Plugin] Duplicate plugin name '{}' in '{}', ignored.",
                            descriptor.name(), jar.getName());
                    continue;
                }
                descriptors.add(descriptor);
            }
        }

        Map<String, Integer> sourceCounts = new HashMap<>();
        for (PluginDescriptor descriptor : descriptors) {
            sourceCounts.merge(descriptor.sourceName().toLowerCase(Locale.ROOT), 1, Integer::sum);
        }
        Set<String> usedSources = new HashSet<>();
        usedSources.add(PermissionRegistry.BUILTIN_SOURCE.toLowerCase(Locale.ROOT));
        for (PluginDescriptor descriptor : descriptors) {
            String display = descriptor.sourceName();
            boolean reserved = PermissionRegistry.BUILTIN_SOURCE.equalsIgnoreCase(display);
            boolean duplicated = sourceCounts.getOrDefault(display.toLowerCase(Locale.ROOT), 1) > 1;
            String source = (reserved || duplicated) ? descriptor.name() : display;
            if (!usedSources.add(source.toLowerCase(Locale.ROOT))) {
                String fallback = descriptor.name();
                if (!usedSources.add(fallback.toLowerCase(Locale.ROOT))) {
                    fallback = descriptor.name() + "-" + Integer.toHexString(descriptor.fileName().hashCode());
                    usedSources.add(fallback.toLowerCase(Locale.ROOT));
                }
                source = fallback;
            }
            PermissionRegistry.getInstance().registerSource(source, remapSource(descriptor.permissions(), source));
            plugins.put(descriptor.name(), new PluginHolder(descriptor));
        }

        Map<String, PluginHolder> byName = new LinkedHashMap<>();
        for (PluginHolder holder : plugins.values()) {
            byName.put(holder.name().toLowerCase(Locale.ROOT), holder);
        }
        for (PluginHolder holder : topoSort(new ArrayList<>(plugins.values()), byName)) {
            PluginStateEntry entry = state.get(holder.name());
            boolean enabled = entry == null || entry.enabled;
            holder.desiredEnabled = enabled;
            if (!enabled) {
                holder.state = PluginState.DISABLED;
                continue;
            }
            List<String> missing = missingDependencies(holder.descriptor(), byName);
            if (!missing.isEmpty()) {
                holder.state = PluginState.MISSING_DEPENDENCY;
                holder.missingDependencies = missing;
                log.warn("[Plugin] '{}' is missing dependencies: {}", holder.name(), missing);
                continue;
            }
            if (!isApiCompatible(holder.descriptor().apiVer())) {
                holder.state = PluginState.INCOMPATIBLE;
                holder.error = holder.descriptor().apiVer();
                log.warn("[Plugin] '{}' requires API {}, server is {}.",
                        holder.name(), holder.descriptor().apiVer(), AppConfig.APP_VERSION);
                continue;
            }
            loadAndEnable(holder);
        }

        bootstrapped = true;
        persistState();
        log.info("[Plugin] Loaded {} plugin(s) from {}", plugins.size(), dir.getAbsolutePath());
    }

    public List<PluginInfo> pluginInfos() {
        List<PluginHolder> holders = snapshot();
        List<PluginInfo> list = new ArrayList<>();
        for (PluginHolder holder : holders) {
            PluginDescriptor d = holder.descriptor();
            list.add(new PluginInfo(
                    d.name(), d.displayName(), d.version(), d.apiVer(),
                    d.authors(), d.website(), d.description(),
                    d.hotReloadable(), holder.state.key(), holder.error,
                    holder.missingDependencies, iconData(holder) != null, holder.menu != null));
        }
        return list;
    }

    public List<PluginMenuEntry> menuEntries() {
        List<PluginMenuEntry> entries = new ArrayList<>();
        for (PluginHolder holder : snapshot()) {
            if (holder.state == PluginState.ENABLED && holder.menu != null) {
                entries.add(new PluginMenuEntry(holder.name(), holder.descriptor().displayName(),
                        holder.menu.id(), holder.menu.title()));
            }
        }
        return entries;
    }

    private synchronized List<PluginHolder> snapshot() {
        return new ArrayList<>(plugins.values());
    }

    public synchronized PluginHolder find(String name) {
        if (name == null) return null;
        for (PluginHolder holder : plugins.values()) {
            if (holder.name().equalsIgnoreCase(name)) return holder;
        }
        return null;
    }

    public PluginIconProvider.IconData iconData(String name) {
        PluginHolder holder = find(name);
        return holder == null ? null : iconData(holder);
    }

    private PluginIconProvider.IconData iconData(PluginHolder holder) {
        if (!holder.iconResolved) {
            synchronized (holder) {
                if (!holder.iconResolved) {
                    holder.icon = PluginIconProvider.load(holder.descriptor());
                    holder.iconResolved = true;
                }
            }
        }
        return holder.icon;
    }

    public synchronized String enable(String name) {
        PluginHolder holder = find(name);
        if (holder == null) return "notFound";
        if (!holder.descriptor().hotReloadable()) return "notHotReloadable";
        if (holder.state == PluginState.ENABLED) return null;

        holder.desiredEnabled = true;
        Map<String, PluginHolder> byName = new LinkedHashMap<>();
        for (PluginHolder h : plugins.values()) byName.put(h.name().toLowerCase(Locale.ROOT), h);
        List<String> missing = missingDependencies(holder.descriptor(), byName);
        if (!missing.isEmpty()) {
            holder.state = PluginState.MISSING_DEPENDENCY;
            holder.missingDependencies = missing;
            return "missingDependency";
        }
        if (!isApiCompatible(holder.descriptor().apiVer())) {
            holder.state = PluginState.INCOMPATIBLE;
            return "incompatible";
        }
        loadAndEnable(holder);
        persistState();
        return holder.state == PluginState.ENABLED ? null : "loadFailed";
    }

    public synchronized String disable(String name) {
        PluginHolder holder = find(name);
        if (holder == null) return "notFound";
        if (!holder.descriptor().hotReloadable()) return "notHotReloadable";
        if (holder.state != PluginState.ENABLED) return "notEnabled";
        unload(holder);
        holder.state = PluginState.DISABLED;
        holder.desiredEnabled = false;
        persistState();
        return null;
    }

    public synchronized String reload(String name) {
        PluginHolder holder = find(name);
        if (holder == null) return "notFound";
        if (!holder.descriptor().hotReloadable()) return "notHotReloadable";
        if (holder.state == PluginState.ENABLED) {
            unload(holder);
            holder.state = PluginState.DISABLED;
        }
        return enable(name);
    }

    public synchronized String renderMenu(String name, String menuId, Context ctx) {
        PluginHolder holder = find(name);
        if (holder == null || holder.state != PluginState.ENABLED || holder.menu == null) return null;
        if (menuId != null && !menuId.equals(holder.menu.id())) return null;
        try {
            PluginRequestImpl request = new PluginRequestImpl(ctx, "");
            return holder.menu.renderer().render(request);
        } catch (Throwable t) {
            log.error("[Plugin] Menu '{}' of '{}' failed: {}", holder.menu.id(), holder.name(), t.getMessage(), t);
            return null;
        }
    }

    public synchronized boolean handleApi(Context ctx, String name, String suffix) {
        PluginHolder holder = find(name);
        if (holder == null || holder.state != PluginState.ENABLED) {
            return false;
        }
        String normalized = suffix == null ? "" : suffix;
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        normalized = normalized.toLowerCase(Locale.ROOT);

        PluginApiHandler handler = holder.routes.get(normalized);
        if (handler == null) {

            int bestLength = -1;
            for (Map.Entry<String, PluginApiHandler> e : holder.routes.entrySet()) {
                String key = e.getKey();
                if (!key.isEmpty() && normalized.startsWith(key + "/") && key.length() > bestLength) {
                    bestLength = key.length();
                    handler = e.getValue();
                }
            }
        }
        if (handler == null) return false;
        try {
            handler.handle(new PluginRequestImpl(ctx, normalized), new PluginResponseImpl(ctx));
        } catch (Throwable t) {
            log.error("[Plugin] API '{}' of '{}' failed: {}", normalized, holder.name(), t.getMessage(), t);
            try {
                if (!ctx.res().isCommitted()) {
                    ctx.status(500).json(Map.of("success", false, "message", "Plugin error"));
                }
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    private void loadAndEnable(PluginHolder holder) {
        PluginClassLoader loader = null;
        LingPlugin plugin = null;
        try {
            loader = new PluginClassLoader(holder.descriptor().jarPath());
            Class<?> mainClass = Class.forName(holder.descriptor().mainClass(), true, loader);
            if (!LingPlugin.class.isAssignableFrom(mainClass)) {
                throw new IllegalStateException("main class does not extend LingPlugin");
            }
            plugin = (LingPlugin) mainClass.getDeclaredConstructor().newInstance();
            PluginContextImpl context = new PluginContextImpl(holder, this);
            holder.pluginLogger = context.loggerImpl();
            plugin.bind(context);
            holder.instance = plugin;
            holder.classLoader = loader;
            plugin.onLoad(context);
            plugin.onEnable();
            holder.state = PluginState.ENABLED;
            holder.error = null;
            log.info("[Plugin] Enabled {} v{}", holder.name(), holder.descriptor().version());
        } catch (Throwable t) {
            if (plugin != null) {
                try {
                    plugin.onDisable();
                } catch (Throwable ignored) {
                }
            }
            closeQuietly(loader);
            closePluginLogger(holder);
            holder.instance = null;
            holder.classLoader = null;
            holder.menu = null;
            holder.routes.clear();
            unregisterPluginRoutes(holder.name());
            holder.state = PluginState.LOAD_FAILED;
            holder.error = t.getClass().getSimpleName();
            log.error("[Plugin] Failed to load '{}': {}", holder.name(),
                    t.getMessage() == null ? holder.error : t.getMessage(), t);
        }
    }

    private void unload(PluginHolder holder) {
        try {
            if (holder.instance != null) {
                holder.instance.onDisable();
            }
        } catch (Throwable t) {
            log.warn("[Plugin] onDisable of '{}' threw: {}", holder.name(), t.getMessage());
        } finally {
            closeQuietly(holder.classLoader);
            closePluginLogger(holder);
            holder.instance = null;
            holder.classLoader = null;
            holder.menu = null;
            holder.routes.clear();
            unregisterPluginRoutes(holder.name());
        }
    }

    public synchronized void shutdownAll() {
        for (PluginHolder holder : plugins.values()) {
            if (holder.state == PluginState.ENABLED) {
                unload(holder);
                holder.state = PluginState.DISABLED;
            }
        }
    }

    private void closeQuietly(PluginClassLoader loader) {
        if (loader == null) return;
        try {
            loader.close();
        } catch (Exception ignored) {
        }
    }

    private void closePluginLogger(PluginHolder holder) {
        PluginLoggerImpl logger = holder.pluginLogger;
        if (logger != null) {
            holder.pluginLogger = null;
            try {
                logger.close();
            } catch (Exception ignored) {
            }
        }
    }

    private List<PluginHolder> topoSort(List<PluginHolder> input, Map<String, PluginHolder> byName) {
        List<PluginHolder> result = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        boolean progress = true;
        while (progress) {
            progress = false;
            for (PluginHolder holder : input) {
                String key = holder.name().toLowerCase(Locale.ROOT);
                if (placed.contains(key)) continue;
                boolean ready = true;
                for (String dep : holder.descriptor().depend()) {
                    String depKey = dep.toLowerCase(Locale.ROOT);
                    if (byName.containsKey(depKey) && !placed.contains(depKey)) {
                        ready = false;
                        break;
                    }
                }
                if (ready) {
                    result.add(holder);
                    placed.add(key);
                    progress = true;
                }
            }
        }
        for (PluginHolder holder : input) {
            if (!placed.contains(holder.name().toLowerCase(Locale.ROOT))) result.add(holder);
        }
        return result;
    }

    private List<String> missingDependencies(PluginDescriptor descriptor, Map<String, PluginHolder> byName) {
        List<String> missing = new ArrayList<>();
        for (String dep : descriptor.depend()) {
            if (!byName.containsKey(dep.toLowerCase(Locale.ROOT))) missing.add(dep);
        }
        return missing;
    }

    private List<PermissionNode> remapSource(List<PermissionNode> nodes, String source) {
        List<PermissionNode> result = new ArrayList<>(nodes.size());
        for (PermissionNode node : nodes) {
            if (source.equals(node.source())) {
                result.add(node);
            } else {
                result.add(new PermissionNode(node.key(), node.type(), node.category(),
                        source, node.description(), node.highRisk()));
            }
        }
        return result;
    }

    static boolean isApiCompatible(String declared) {
        if (declared == null || declared.isBlank()) return false;
        String server = AppConfig.APP_VERSION;
        String value = declared.trim();
        java.util.regex.Matcher range = API_RANGE_PATTERN.matcher(value);
        if (range.matches()) {
            return compareVersions(server, range.group(1)) >= 0
                    && compareVersions(server, range.group(2)) <= 0;
        }
        return compareVersions(server, value) == 0;
    }

    private static int compareVersions(String a, String b) {
        String[] pa = a.split("[.-]");
        String[] pb = b.split("[.-]");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int va = i < pa.length ? parseInt(pa[i]) : 0;
            int vb = i < pb.length ? parseInt(pb[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9].*$", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public static final class PluginStateEntry {
        public String version;
        public String friendlyName;
        public boolean enabled = true;
        public String path;
        public String updatedAt;
    }

    private Map<String, PluginStateEntry> loadState() {
        Map<String, PluginStateEntry> state = new LinkedHashMap<>();
        if (stateFile == null || !stateFile.exists()) return state;
        try {
            byte[] bytes = Files.readAllBytes(stateFile.toPath());
            Map<String, PluginStateEntry> loaded = mapper.readValue(
                    new String(bytes, StandardCharsets.UTF_8),
                    new TypeReference<>() {
                    });
            if (loaded != null) state.putAll(loaded);
        } catch (Exception e) {
            log.warn("[Plugin] Failed to read {}: {}", STATE_FILE, e.getMessage());
        }
        return state;
    }

    private void persistState() {
        if (stateFile == null) return;
        for (PluginHolder holder : plugins.values()) {
            PluginStateEntry entry = state.computeIfAbsent(holder.name(), k -> new PluginStateEntry());
            entry.version = holder.descriptor().version();
            entry.friendlyName = holder.descriptor().friendlyName();
            entry.path = holder.descriptor().fileName();
            entry.enabled = holder.desiredEnabled;
            entry.updatedAt = Instant.now().toString();
        }
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(state);
            Files.writeString(stateFile.toPath(), json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[Plugin] Failed to write {}: {}", STATE_FILE, e.getMessage());
        }
    }
}
