package net.axther.hydroxide.modules;

import net.axther.hydroxide.HydroxideContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ModuleManager {

    private final ModuleConfig config;
    private final Map<String, HydroModule> modules = new LinkedHashMap<>();
    private final Map<String, ModuleStatus> statuses = new HashMap<>();
    private final List<String> enabledOrder = new ArrayList<>();

    public ModuleManager(ModuleConfig config) {
        this.config = config;
    }

    public void register(HydroModule module) {
        String id = normalize(module.id());
        if (modules.containsKey(id)) {
            throw new IllegalArgumentException("Module already registered: " + id);
        }
        modules.put(id, module);
        statuses.put(id, ModuleStatus.REGISTERED);
    }

    public void enableConfiguredModules(HydroxideContext context) {
        enabledOrder.clear();
        modules.forEach((id, module) -> statuses.put(id, config.isEnabled(id, module.defaultEnabled())
                ? ModuleStatus.REGISTERED
                : ModuleStatus.DISABLED_BY_CONFIG));

        for (String id : modules.keySet()) {
            enableWithDependencies(context, id, new ArrayDeque<>());
        }
    }

    public void reloadEnabledModules(HydroxideContext context) {
        for (String id : enabledOrder) {
            modules.get(id).onReload(context);
        }
    }

    public void disableAll(HydroxideContext context) {
        List<String> reverseOrder = new ArrayList<>(enabledOrder);
        Collections.reverse(reverseOrder);
        for (String id : reverseOrder) {
            modules.get(id).onDisable(context);
            statuses.put(id, ModuleStatus.DISABLED);
        }
        enabledOrder.clear();
    }

    public ModuleStatus status(String moduleId) {
        return statuses.getOrDefault(normalize(moduleId), ModuleStatus.MISSING_DEPENDENCY);
    }

    public List<String> enabledModuleIds() {
        return List.copyOf(enabledOrder);
    }

    public Collection<HydroModule> registeredModules() {
        return List.copyOf(modules.values());
    }

    public Optional<HydroModule> module(String moduleId) {
        return Optional.ofNullable(modules.get(normalize(moduleId)));
    }

    private boolean enableWithDependencies(HydroxideContext context, String id, ArrayDeque<String> stack) {
        ModuleStatus status = statuses.get(id);
        if (status == ModuleStatus.ENABLED) {
            return true;
        }
        if (status == ModuleStatus.DISABLED_BY_CONFIG
                || status == ModuleStatus.MISSING_DEPENDENCY
                || status == ModuleStatus.FAILED) {
            return false;
        }
        if (stack.contains(id)) {
            statuses.put(id, ModuleStatus.MISSING_DEPENDENCY);
            return false;
        }

        HydroModule module = modules.get(id);
        if (module == null) {
            return false;
        }

        stack.push(id);
        for (String dependency : module.dependencies()) {
            String dependencyId = normalize(dependency);
            if (!modules.containsKey(dependencyId) || !enableWithDependencies(context, dependencyId, stack)) {
                statuses.put(id, ModuleStatus.MISSING_DEPENDENCY);
                stack.pop();
                return false;
            }
        }
        stack.pop();

        try {
            module.onEnable(context);
            statuses.put(id, ModuleStatus.ENABLED);
            enabledOrder.add(id);
            return true;
        } catch (RuntimeException exception) {
            statuses.put(id, ModuleStatus.FAILED);
            throw exception;
        }
    }

    private String normalize(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}
