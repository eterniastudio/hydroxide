package net.axther.hydroxide.commands.framework;

import net.axther.hydroxide.messages.MessageService;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class CommandContext {

    private final CommandActor actor;
    private final MessageService messages;
    private final String label;
    private final List<String> arguments;
    private final CommandArgumentParser parser;

    CommandContext(CommandActor actor, MessageService messages, String label, List<String> arguments, CommandArgumentParser parser) {
        this.actor = actor;
        this.messages = messages;
        this.label = label;
        this.arguments = List.copyOf(arguments);
        this.parser = parser;
    }

    public CommandActor actor() {
        return actor;
    }

    public CommandSender sender() {
        return actor.sender().orElseThrow(() -> new IllegalStateException("No Bukkit sender is attached to this command context."));
    }

    public MessageService messages() {
        return messages;
    }

    public String label() {
        return label;
    }

    public List<String> arguments() {
        return arguments;
    }

    public String argument(int index) {
        return index >= 0 && index < arguments.size() ? arguments.get(index) : "";
    }

    public CommandArgumentParser parser() {
        return parser;
    }

    public void message(String key, Map<String, ?> placeholders) {
        actor.send(messages.prefixedComponent(key, placeholders));
    }
}
