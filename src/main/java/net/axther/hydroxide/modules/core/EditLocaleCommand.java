package net.axther.hydroxide.modules.core;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.messages.MessageService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

final class EditLocaleCommand {

    private static final int SEARCH_LIMIT = 10;
    private static final int PREVIEW_LIMIT = 160;
    private static final Pattern MESSAGE_KEY = Pattern.compile("[A-Za-z0-9_.-]+");

    private final HydroxideContext context;

    EditLocaleCommand(HydroxideContext context) {
        this.context = context;
    }

    CommandService command() {
        return new CommandService(HydroCommand.builder("editlocale")
                .permission("hydroxide.command.editlocale")
                .usage("/{label} [keyword|reload|set <key> <value>]")
                .executor(this::execute)
                .completer(this::complete)
                .build(), context.messages());
    }

    private void execute(CommandContext ctx) {
        if (ctx.arguments().isEmpty()) {
            search(ctx, "");
            return;
        }
        String action = ctx.argument(0);
        if (action.equalsIgnoreCase("reload")) {
            if (ctx.arguments().size() != 1) {
                ctx.message("core.locale.usage", Map.of("label", ctx.label()));
                return;
            }
            context.messages().reload();
            ctx.message("core.messages.reload-success", Map.of());
            return;
        }
        if (action.equalsIgnoreCase("set")) {
            set(ctx);
            return;
        }
        if (ctx.arguments().size() > 1) {
            ctx.message("core.locale.usage", Map.of("label", ctx.label()));
            return;
        }
        search(ctx, ctx.argument(0));
    }

    private void set(CommandContext ctx) {
        Optional<EditLocaleCommandParser.SetRequest> parsed = EditLocaleCommandParser.parseSet(ctx.arguments());
        if (parsed.isEmpty()) {
            ctx.message("core.locale.set-usage", Map.of("label", ctx.label()));
            return;
        }
        EditLocaleCommandParser.SetRequest request = parsed.orElseThrow();
        if (!context.messages().fileBacked()) {
            ctx.message("core.locale.not-file-backed", Map.of());
            return;
        }
        if (!MESSAGE_KEY.matcher(request.key()).matches()) {
            ctx.message("core.locale.invalid-key", Map.of("key", request.key()));
            return;
        }
        if (!context.messages().hasStringKey(request.key())) {
            ctx.message("core.locale.missing-key", Map.of("key", request.key()));
            return;
        }
        try {
            File backup = context.messages().setRawTemplate(request.key(), request.value());
            ctx.message("core.locale.set", Map.of(
                    "key", request.key(),
                    "value", context.text().literal(preview(request.value())),
                    "backup", backup.getName()
            ));
        } catch (IOException | RuntimeException exception) {
            ctx.message("core.locale.save-failed", Map.of("reason", exception.getMessage()));
        }
    }

    private void search(CommandContext ctx, String query) {
        List<MessageService.MessageEntry> results = context.messages().search(query, SEARCH_LIMIT);
        if (results.isEmpty()) {
            ctx.message("core.locale.empty", Map.of("query", queryDisplay(query)));
            return;
        }

        ctx.message("core.locale.header", Map.of(
                "query", queryDisplay(query),
                "count", results.size()
        ));
        for (MessageService.MessageEntry entry : results) {
            ctx.message("core.locale.entry", Map.of(
                    "key", entry.key(),
                    "value", context.text().literal(preview(entry.value()))
            ));
        }
    }

    private List<String> complete(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            String prefix = ctx.argument(0).toLowerCase(Locale.ROOT);
            List<String> candidates = new ArrayList<>();
            candidates.add("reload");
            candidates.add("set");
            context.messages().stringKeys().stream()
                    .filter(key -> key.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .limit(25)
                    .forEach(candidates::add);
            return candidates.stream()
                    .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        if (ctx.arguments().size() == 2 && ctx.argument(0).equalsIgnoreCase("set")) {
            String prefix = ctx.argument(1).toLowerCase(Locale.ROOT);
            return context.messages().stringKeys().stream()
                    .filter(key -> key.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .limit(25)
                    .toList();
        }
        return List.of();
    }

    private String queryDisplay(String query) {
        return query == null || query.isBlank() ? "*" : query;
    }

    private String preview(String value) {
        String singleLine = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (singleLine.length() <= PREVIEW_LIMIT) {
            return singleLine;
        }
        return singleLine.substring(0, PREVIEW_LIMIT - 3) + "...";
    }
}
