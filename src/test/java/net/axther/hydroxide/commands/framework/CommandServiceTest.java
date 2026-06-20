package net.axther.hydroxide.commands.framework;

import net.axther.hydroxide.messages.MessageService;
import net.axther.hydroxide.text.TextFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandServiceTest {

    private final TextFormatter formatter = new TextFormatter();

    @Test
    void executesNestedSubcommandWithSlicedArguments() {
        AtomicReference<List<String>> captured = new AtomicReference<>();
        HydroCommand root = HydroCommand.builder("hydroxide")
                .permission("hydroxide.command.hydroxide")
                .executor(ctx -> captured.set(ctx.arguments()))
                .child(HydroCommand.builder("messages")
                        .child(HydroCommand.builder("reload")
                                .permission("hydroxide.command.messages.reload")
                                .executor(ctx -> captured.set(ctx.arguments()))
                                .build())
                        .build())
                .build();
        FakeActor actor = new FakeActor("Admin", true, "hydroxide.command.hydroxide", "hydroxide.command.messages.reload");

        new CommandService(root, messages(), () -> 0L).execute(actor, "hydroxide", new String[]{"messages", "reload", "now"});

        assertEquals(List.of("now"), captured.get());
    }

    @Test
    void sendsNoPermissionMessageAndDoesNotExecute() {
        AtomicInteger executions = new AtomicInteger();
        HydroCommand root = HydroCommand.builder("hydroxide")
                .permission("hydroxide.command.hydroxide")
                .executor(ctx -> executions.incrementAndGet())
                .build();
        FakeActor actor = new FakeActor("Guest", true);

        new CommandService(root, messages(), () -> 0L).execute(actor, "hydroxide", new String[0]);

        assertEquals(0, executions.get());
        assertEquals(List.of("Hydroxide > No permission: hydroxide.command.hydroxide"), actor.messages(formatter));
    }

    @Test
    void blocksPlayerOnlyCommandForConsoleActors() {
        AtomicInteger executions = new AtomicInteger();
        HydroCommand root = HydroCommand.builder("home")
                .playerOnly(true)
                .executor(ctx -> executions.incrementAndGet())
                .build();
        FakeActor actor = new FakeActor("Console", false);

        new CommandService(root, messages(), () -> 0L).execute(actor, "home", new String[0]);

        assertEquals(0, executions.get());
        assertEquals(List.of("Hydroxide > Only players can use that command."), actor.messages(formatter));
    }

    @Test
    void enforcesCooldownPerActorAndCommand() {
        AtomicInteger executions = new AtomicInteger();
        AtomicLong now = new AtomicLong(1_000L);
        HydroCommand root = HydroCommand.builder("rtp")
                .cooldown(Duration.ofSeconds(5))
                .executor(ctx -> executions.incrementAndGet())
                .build();
        CommandService service = new CommandService(root, messages(), now::get);
        FakeActor actor = new FakeActor("Steve", true);

        service.execute(actor, "rtp", new String[0]);
        service.execute(actor, "rtp", new String[0]);
        now.addAndGet(5_000L);
        service.execute(actor, "rtp", new String[0]);

        assertEquals(2, executions.get());
        assertEquals(List.of("Hydroxide > Wait 5s before using that again."), actor.messages(formatter));
    }

    @Test
    void tabCompletionFiltersByPermissionAndCompletesNestedChildren() {
        HydroCommand root = HydroCommand.builder("hydroxide")
                .child(HydroCommand.builder("modules").permission("hydroxide.command.modules").executor(ctx -> { }).build())
                .child(HydroCommand.builder("messages")
                        .permission("hydroxide.command.hydroxide")
                        .child(HydroCommand.builder("reload").permission("hydroxide.command.messages.reload").executor(ctx -> { }).build())
                        .build())
                .build();
        CommandService service = new CommandService(root, messages(), () -> 0L);

        assertEquals(List.of("messages"), service.complete(new FakeActor("Admin", true, "hydroxide.command.hydroxide"), "hydroxide", new String[]{"m"}));
        assertEquals(List.of("reload"), service.complete(new FakeActor("Admin", true, "hydroxide.command.hydroxide", "hydroxide.command.messages.reload"), "hydroxide", new String[]{"messages", "r"}));
    }

    @Test
    void tabCompletionUsesRootCompleterWhenCommandHasNoSubcommands() {
        HydroCommand root = HydroCommand.builder("pay")
                .completer(ctx -> List.of("Alex", "Steve").stream()
                        .filter(name -> name.toLowerCase().startsWith(ctx.argument(0).toLowerCase()))
                        .toList())
                .executor(ctx -> { })
                .build();
        CommandService service = new CommandService(root, messages(), () -> 0L);

        assertEquals(List.of("Alex"), service.complete(new FakeActor("Admin", true), "pay", new String[]{"a"}));
    }

    private MessageService messages() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("core.prefix", "<#44CCFF>Hydroxide <dark_gray>> <gray>");
        yaml.set("validation.no-permission", "<red>No permission: {permission}");
        yaml.set("validation.player-only", "<red>Only players can use that command.");
        yaml.set("validation.console-only", "<red>Only console can use that command.");
        yaml.set("validation.cooldown", "<red>Wait {remaining} before using that again.");
        yaml.set("validation.usage", "<red>Usage: {usage}");
        return new MessageService(formatter, yaml);
    }

    private static final class FakeActor implements CommandActor {
        private final String name;
        private final boolean player;
        private final Set<String> permissions;
        private final List<Component> messages = new ArrayList<>();

        private FakeActor(String name, boolean player, String... permissions) {
            this.name = name;
            this.player = player;
            this.permissions = new HashSet<>(List.of(permissions));
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean isPlayer() {
            return player;
        }

        @Override
        public boolean hasPermission(String permission) {
            return permissions.contains(permission);
        }

        @Override
        public void send(Component component) {
            messages.add(component);
        }

        private List<String> messages(TextFormatter formatter) {
            return messages.stream().map(formatter::plain).toList();
        }
    }
}
