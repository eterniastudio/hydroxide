package net.axther.hydroxide.modules.core;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

final class HelpCommand {

    private final HydroxideContext context;

    HelpCommand(HydroxideContext context) {
        this.context = context;
    }

    CommandService command() {
        return new CommandService(HydroCommand.builder("help")
                .permission("hydroxide.command.help")
                .usage("/{label} [page|query]")
                .executor(ctx -> help(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> ctx.arguments().size() == 1
                        ? CommandUtils.matching(ctx.argument(0), commandNames(ctx.sender()))
                        : List.of())
                .build(), context.messages());
    }

    private void help(CommandSender sender, String label, List<String> args) {
        String query = "";
        int page = 1;
        if (!args.isEmpty()) {
            Integer requestedPage = parsePage(args.getFirst());
            if (requestedPage == null) {
                query = String.join(" ", args);
            } else {
                page = requestedPage;
            }
        }

        CoreHelpIndex index = CoreHelpIndex.from(pluginYml());
        List<CoreHelpIndex.Entry> matches = index.find(query, sender::hasPermission);
        if (matches.isEmpty()) {
            context.message(sender, "help.empty", Map.of("query", query.isBlank() ? "*" : query));
            return;
        }

        int pageSize = Math.max(1, context.plugin().getConfig().getInt("help.page-size", 8));
        CoreHelpIndex.Page visible = CoreHelpIndex.page(matches, page, pageSize);
        context.message(sender, "help.header", Map.of(
                "page", visible.page(),
                "total_pages", visible.totalPages(),
                "query", query.isBlank() ? "*" : query
        ));
        for (CoreHelpIndex.Entry entry : visible.entries()) {
            context.message(sender, "help.entry", Map.of(
                    "command", entry.name(),
                    "usage", entry.usage(),
                    "description", entry.description()
            ));
        }
        context.message(sender, "help.footer", Map.of(
                "label", label,
                "page", visible.page(),
                "total_pages", visible.totalPages(),
                "next_page", Math.min(visible.totalPages(), visible.page() + 1),
                "previous_page", Math.max(1, visible.page() - 1)
        ));
    }

    private List<String> commandNames(CommandSender sender) {
        return CoreHelpIndex.from(pluginYml()).find("", sender::hasPermission).stream()
                .map(CoreHelpIndex.Entry::name)
                .toList();
    }

    private Integer parsePage(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private YamlConfiguration pluginYml() {
        InputStream stream = context.plugin().getResource("plugin.yml");
        if (stream == null) {
            return new YamlConfiguration();
        }
        try (InputStream input = stream;
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            context.plugin().getLogger().warning("Unable to read bundled plugin.yml help metadata: " + exception.getMessage());
            return new YamlConfiguration();
        }
    }
}
