package net.axther.hydroxide.modules;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleManagerTest {

    @Test
    void enablesConfiguredModulesInDependencyOrder() {
        List<String> events = new ArrayList<>();
        ModuleManager manager = new ModuleManager(ModuleConfig.overrides(Map.of()));

        manager.register(new RecordingModule("chat", List.of("core"), true, events));
        manager.register(new RecordingModule("core", List.of(), true, events));

        manager.enableConfiguredModules(null);

        assertEquals(List.of("enable:core", "enable:chat"), events);
        assertEquals(List.of("core", "chat"), manager.enabledModuleIds());
        assertEquals(ModuleStatus.ENABLED, manager.status("chat"));
    }

    @Test
    void skipsModulesDisabledByConfig() {
        List<String> events = new ArrayList<>();
        ModuleManager manager = new ModuleManager(ModuleConfig.overrides(Map.of("chat", false)));

        manager.register(new RecordingModule("core", List.of(), true, events));
        manager.register(new RecordingModule("chat", List.of("core"), true, events));

        manager.enableConfiguredModules(null);

        assertEquals(List.of("enable:core"), events);
        assertEquals(ModuleStatus.DISABLED_BY_CONFIG, manager.status("chat"));
    }

    @Test
    void skipsModulesWithDisabledDependencies() {
        List<String> events = new ArrayList<>();
        ModuleManager manager = new ModuleManager(ModuleConfig.overrides(Map.of("core", false, "chat", true)));

        manager.register(new RecordingModule("core", List.of(), true, events));
        manager.register(new RecordingModule("chat", List.of("core"), true, events));

        manager.enableConfiguredModules(null);

        assertEquals(List.of(), events);
        assertEquals(ModuleStatus.DISABLED_BY_CONFIG, manager.status("core"));
        assertEquals(ModuleStatus.MISSING_DEPENDENCY, manager.status("chat"));
    }

    @Test
    void reloadsOnlyEnabledModules() {
        List<String> events = new ArrayList<>();
        ModuleManager manager = new ModuleManager(ModuleConfig.overrides(Map.of("chat", false)));

        manager.register(new RecordingModule("core", List.of(), true, events));
        manager.register(new RecordingModule("chat", List.of("core"), true, events));

        manager.enableConfiguredModules(null);
        events.clear();
        manager.reloadEnabledModules(null);

        assertEquals(List.of("reload:core"), events);
    }

    private record RecordingModule(
            String id,
            List<String> dependencies,
            boolean defaultEnabled,
            List<String> events
    ) implements HydroModule {

        @Override
        public String displayName() {
            return id;
        }

        @Override
        public String description() {
            return id + " module";
        }

        @Override
        public void onEnable(net.axther.hydroxide.HydroxideContext context) {
            events.add("enable:" + id);
        }

        @Override
        public void onDisable(net.axther.hydroxide.HydroxideContext context) {
            events.add("disable:" + id);
        }

        @Override
        public void onReload(net.axther.hydroxide.HydroxideContext context) {
            events.add("reload:" + id);
        }
    }
}
