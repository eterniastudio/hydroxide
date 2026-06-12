package net.axther.hydroxide.commands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public final class CommandRegistrar {

    private final JavaPlugin plugin;
    private static final TabCompleter EMPTY_COMPLETER = (sender, command, alias, args) -> List.of();

    public CommandRegistrar(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(String name, CommandExecutor executor) {
        register(name, executor, executor instanceof TabCompleter completer ? completer : null);
    }

    public void register(String name, CommandExecutor executor, TabCompleter completer) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().log(Level.WARNING, "Command /{0} is missing from plugin.yml", name);
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(completer == null ? EMPTY_COMPLETER : completer);
    }
}
