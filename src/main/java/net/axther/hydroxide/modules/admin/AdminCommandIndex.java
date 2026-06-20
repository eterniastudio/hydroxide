package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class AdminCommandIndex {

    private final List<CommandInfo> commands;

    private AdminCommandIndex(List<CommandInfo> commands) {
        this.commands = commands.stream()
                .sorted(Comparator.comparing(CommandInfo::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static AdminCommandIndex from(YamlConfiguration yaml) {
        ConfigurationSection section = yaml.getConfigurationSection("commands");
        if (section == null) {
            return new AdminCommandIndex(List.of());
        }
        List<CommandInfo> commands = section.getKeys(false).stream()
                .map(command -> new CommandInfo(
                        command,
                        section.getStringList(command + ".aliases"),
                        section.getString(command + ".description", ""),
                        section.getString(command + ".usage", ""),
                        section.getString(command + ".permission", "")
                ))
                .toList();
        return new AdminCommandIndex(commands);
    }

    List<CommandInfo> find(String keyword) {
        String lowered = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        if (lowered.isBlank()) {
            return commands;
        }
        return commands.stream()
                .filter(command -> command.matches(lowered))
                .toList();
    }

    record CommandInfo(String name, List<String> aliases, String description, String usage, String permission) {

        private boolean matches(String keyword) {
            return name.toLowerCase(Locale.ROOT).contains(keyword)
                    || aliases.stream().anyMatch(alias -> alias.toLowerCase(Locale.ROOT).contains(keyword))
                    || description.toLowerCase(Locale.ROOT).contains(keyword)
                    || usage.toLowerCase(Locale.ROOT).contains(keyword)
                    || permission.toLowerCase(Locale.ROOT).contains(keyword);
        }
    }
}
