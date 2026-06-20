package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminCommandIndexTest {

    @Test
    void findsCommandsByNameAliasDescriptionUsageOrPermission() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("commands.fly.description", "Toggle flight for a player.");
        yaml.set("commands.fly.usage", "/fly [player]");
        yaml.set("commands.fly.permission", "hydroxide.command.fly");
        yaml.set("commands.fly.aliases", List.of("flight"));
        yaml.set("commands.give.description", "Give an item.");
        yaml.set("commands.give.usage", "/give <player> <item>");
        yaml.set("commands.give.permission", "hydroxide.admin.give");

        AdminCommandIndex index = AdminCommandIndex.from(yaml);

        assertEquals(List.of("fly"), index.find("flight").stream().map(AdminCommandIndex.CommandInfo::name).toList());
        assertEquals(List.of("fly"), index.find("toggle").stream().map(AdminCommandIndex.CommandInfo::name).toList());
        assertEquals(List.of("give"), index.find("admin.give").stream().map(AdminCommandIndex.CommandInfo::name).toList());
    }

    @Test
    void emptyKeywordReturnsSortedCommands() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("commands.zeta.description", "Last");
        yaml.set("commands.alpha.description", "First");

        assertEquals(List.of("alpha", "zeta"), AdminCommandIndex.from(yaml).find("").stream()
                .map(AdminCommandIndex.CommandInfo::name)
                .toList());
    }

    @Test
    void missingCommandSectionReturnsNoMatches() {
        assertTrue(AdminCommandIndex.from(new YamlConfiguration()).find("anything").isEmpty());
    }
}
