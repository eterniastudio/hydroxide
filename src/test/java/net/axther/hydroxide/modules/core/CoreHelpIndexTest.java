package net.axther.hydroxide.modules.core;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreHelpIndexTest {

    @Test
    void findsVisibleCommandsByPermissionAndKeyword() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("commands.home.description", "Teleport to a saved home.");
        yaml.set("commands.home.usage", "/home <name>");
        yaml.set("commands.home.permission", "hydroxide.command.home");
        yaml.set("commands.home.aliases", List.of("homes"));
        yaml.set("commands.ban.description", "Ban a player.");
        yaml.set("commands.ban.usage", "/ban <player>");
        yaml.set("commands.ban.permission", "hydroxide.command.ban");

        CoreHelpIndex index = CoreHelpIndex.from(yaml);

        List<String> visible = index.find("teleport", Set.of("hydroxide.command.home")::contains)
                .stream()
                .map(CoreHelpIndex.Entry::name)
                .toList();

        assertEquals(List.of("home"), visible);
        assertTrue(index.find("ban", Set.of("hydroxide.command.home")::contains).isEmpty());
    }

    @Test
    void blankQueryReturnsSortedVisibleCommands() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("commands.zeta.description", "Last");
        yaml.set("commands.alpha.description", "First");

        assertEquals(List.of("alpha", "zeta"), CoreHelpIndex.from(yaml).find("", permission -> false).stream()
                .map(CoreHelpIndex.Entry::name)
                .toList());
    }

    @Test
    void pagesResultsWithOneBasedPages() {
        List<CoreHelpIndex.Entry> entries = List.of(
                entry("a"),
                entry("b"),
                entry("c")
        );

        CoreHelpIndex.Page page = CoreHelpIndex.page(entries, 2, 2);

        assertEquals(2, page.page());
        assertEquals(2, page.totalPages());
        assertEquals(List.of("c"), page.entries().stream().map(CoreHelpIndex.Entry::name).toList());
    }

    private static CoreHelpIndex.Entry entry(String name) {
        return new CoreHelpIndex.Entry(name, List.of(), "desc", "/" + name, "");
    }
}
