package net.axther.hydroxide.modules.environment;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EnvironmentModule implements HydroModule {

    private static final List<String> TIME_VALUES = List.of("day", "noon", "sunset", "night", "midnight", "sunrise");
    private static final List<String> WEATHER_VALUES = List.of("clear", "sun", "rain", "storm", "thunder");
    private static final List<String> PERSONAL_TIME_VALUES = List.of("reset", "day", "noon", "sunset", "night", "midnight", "sunrise");
    private static final List<String> PERSONAL_WEATHER_VALUES = List.of("reset", "clear", "sun", "rain", "storm", "thunder");
    private static final List<String> DURATION_VALUES = List.of("60", "300", "600", "1800", "3600");

    private HydroxideContext context;

    @Override
    public String id() {
        return "environment";
    }

    @Override
    public String displayName() {
        return "Time and Weather";
    }

    @Override
    public String description() {
        return "Essentials-style world time and weather controls.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        context.commands().register("time", command("time", "hydroxide.command.time", "/{label} <day|noon|night|midnight|ticks> [world]",
                ctx -> time(ctx.sender(), ctx.label(), ctx.arguments()), this::timeCompletions));
        context.commands().register("day", command("day", "hydroxide.command.time", "/{label} [world]",
                ctx -> timeShortcut(ctx.sender(), "day", ctx.arguments()), this::shortcutWorldCompletions));
        context.commands().register("night", command("night", "hydroxide.command.time", "/{label} [world]",
                ctx -> timeShortcut(ctx.sender(), "night", ctx.arguments()), this::shortcutWorldCompletions));
        context.commands().register("weather", command("weather", "hydroxide.command.weather", "/{label} <sun|storm|thunder> [durationSeconds] [world]",
                ctx -> weather(ctx.sender(), ctx.label(), ctx.arguments()), this::weatherCompletions));
        context.commands().register("sun", command("sun", "hydroxide.command.weather", "/{label} [durationSeconds] [world]",
                ctx -> weatherShortcut(ctx.sender(), EnvironmentWeatherMode.CLEAR, ctx.arguments()), this::weatherShortcutCompletions));
        context.commands().register("storm", command("storm", "hydroxide.command.weather", "/{label} [durationSeconds] [world]",
                ctx -> weatherShortcut(ctx.sender(), EnvironmentWeatherMode.RAIN, ctx.arguments()), this::weatherShortcutCompletions));
        context.commands().register("thunder", command("thunder", "hydroxide.command.weather", "/{label} [durationSeconds] [world]",
                ctx -> weatherShortcut(ctx.sender(), EnvironmentWeatherMode.THUNDER, ctx.arguments()), this::weatherShortcutCompletions));
        context.commands().register("ptime", command("ptime", "hydroxide.command.ptime", "/{label} <reset|day|noon|night|midnight|ticks> [player]",
                ctx -> personalTime(ctx.sender(), ctx.label(), ctx.arguments()), this::personalTimeCompletions));
        context.commands().register("pweather", command("pweather", "hydroxide.command.pweather", "/{label} <reset|sun|storm|thunder> [player]",
                ctx -> personalWeather(ctx.sender(), ctx.label(), ctx.arguments()), this::personalWeatherCompletions));
    }

    private CommandService command(String name, String permission, String usage, HydroCommand.HydroCommandExecutor executor,
                                   HydroCommand.HydroTabCompleter completer) {
        return new CommandService(HydroCommand.builder(name)
                .permission(permission)
                .usage(usage)
                .executor(executor)
                .completer(completer)
                .build(), context.messages());
    }

    private void time(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(sender, "environment.time.usage", Map.of("label", label));
            return;
        }
        Optional<Long> time = EnvironmentCommandParser.time(args.get(0));
        if (time.isEmpty()) {
            context.message(sender, "environment.time.invalid", Map.of("value", args.get(0)));
            return;
        }
        Optional<World> world = world(sender, args, 1);
        if (world.isEmpty()) {
            context.message(sender, "environment.world-not-found", Map.of("world", args.size() > 1 ? args.get(1) : ""));
            return;
        }
        applyTime(sender, world.get(), time.get(), args.get(0));
    }

    private void timeShortcut(CommandSender sender, String value, List<String> args) {
        Optional<World> world = world(sender, args, 0);
        if (world.isEmpty()) {
            context.message(sender, "environment.world-not-found", Map.of("world", args.isEmpty() ? "" : args.get(0)));
            return;
        }
        applyTime(sender, world.get(), EnvironmentCommandParser.time(value).orElseThrow(), value);
    }

    private void applyTime(CommandSender sender, World world, long ticks, String value) {
        world.setTime(ticks);
        context.message(sender, "environment.time.changed", Map.of("world", world.getName(), "time", value, "ticks", ticks));
    }

    private void weather(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(sender, "environment.weather.usage", Map.of("label", label));
            return;
        }
        Optional<EnvironmentWeatherMode> mode = EnvironmentCommandParser.weather(args.get(0));
        if (mode.isEmpty()) {
            context.message(sender, "environment.weather.invalid", Map.of("value", args.get(0)));
            return;
        }
        WeatherArgs parsed = parseWeatherArgs(args, 1);
        Optional<World> world = world(sender, args, parsed.worldIndex());
        if (world.isEmpty()) {
            context.message(sender, "environment.world-not-found", Map.of("world", parsed.worldIndex() < args.size() ? args.get(parsed.worldIndex()) : ""));
            return;
        }
        applyWeather(sender, world.get(), mode.get(), parsed.durationSeconds());
    }

    private void weatherShortcut(CommandSender sender, EnvironmentWeatherMode mode, List<String> args) {
        WeatherArgs parsed = parseWeatherArgs(args, 0);
        Optional<World> world = world(sender, args, parsed.worldIndex());
        if (world.isEmpty()) {
            context.message(sender, "environment.world-not-found", Map.of("world", parsed.worldIndex() < args.size() ? args.get(parsed.worldIndex()) : ""));
            return;
        }
        applyWeather(sender, world.get(), mode, parsed.durationSeconds());
    }

    private WeatherArgs parseWeatherArgs(List<String> args, int startIndex) {
        int duration = context.plugin().getConfig().getInt("environment.default-weather-duration-seconds", 600);
        int worldIndex = startIndex;
        if (args.size() > startIndex) {
            Optional<Integer> parsed = positiveInt(args.get(startIndex));
            if (parsed.isPresent()) {
                duration = parsed.get();
                worldIndex = startIndex + 1;
            }
        }
        return new WeatherArgs(Math.max(1, duration), worldIndex);
    }

    private void applyWeather(CommandSender sender, World world, EnvironmentWeatherMode mode, int durationSeconds) {
        int durationTicks = (int) Math.min(Integer.MAX_VALUE, Math.max(20L, durationSeconds * 20L));
        switch (mode) {
            case CLEAR -> {
                world.setStorm(false);
                world.setThundering(false);
                world.setClearWeatherDuration(durationTicks);
            }
            case RAIN -> {
                world.setStorm(true);
                world.setThundering(false);
                world.setWeatherDuration(durationTicks);
            }
            case THUNDER -> {
                world.setStorm(true);
                world.setThundering(true);
                world.setThunderDuration(durationTicks);
            }
        }
        context.message(sender, "environment.weather.changed", Map.of(
                "world", world.getName(),
                "weather", mode.name().toLowerCase(java.util.Locale.ROOT),
                "duration", durationSeconds
        ));
    }

    private void personalTime(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(sender, "environment.ptime.usage", Map.of("label", label));
            return;
        }
        Optional<Player> target = target(sender, args, 1);
        if (target.isEmpty()) {
            return;
        }
        Player player = target.get();
        if (args.get(0).equalsIgnoreCase("reset")) {
            player.resetPlayerTime();
            context.message(sender, "environment.ptime.reset", Map.of("target", player.getName()));
            return;
        }
        Optional<Long> time = EnvironmentCommandParser.time(args.get(0));
        if (time.isEmpty()) {
            context.message(sender, "environment.ptime.invalid", Map.of("value", args.get(0)));
            return;
        }
        player.setPlayerTime(time.get(), false);
        context.message(sender, "environment.ptime.changed", Map.of(
                "target", player.getName(),
                "time", args.get(0),
                "ticks", time.get()
        ));
    }

    private void personalWeather(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            context.message(sender, "environment.pweather.usage", Map.of("label", label));
            return;
        }
        Optional<Player> target = target(sender, args, 1);
        if (target.isEmpty()) {
            return;
        }
        Player player = target.get();
        if (args.get(0).equalsIgnoreCase("reset")) {
            player.resetPlayerWeather();
            context.message(sender, "environment.pweather.reset", Map.of("target", player.getName()));
            return;
        }
        Optional<EnvironmentWeatherMode> mode = EnvironmentCommandParser.weather(args.get(0));
        if (mode.isEmpty()) {
            context.message(sender, "environment.pweather.invalid", Map.of("value", args.get(0)));
            return;
        }
        player.setPlayerWeather(mode.get() == EnvironmentWeatherMode.CLEAR ? WeatherType.CLEAR : WeatherType.DOWNFALL);
        context.message(sender, "environment.pweather.changed", Map.of(
                "target", player.getName(),
                "weather", mode.get().name().toLowerCase(java.util.Locale.ROOT)
        ));
    }

    private Optional<World> world(CommandSender sender, List<String> args, int index) {
        if (index < args.size()) {
            return Optional.ofNullable(Bukkit.getWorld(args.get(index)));
        }
        if (sender instanceof Player player) {
            return Optional.of(player.getWorld());
        }
        return Bukkit.getWorlds().stream().findFirst();
    }

    private Optional<Player> target(CommandSender sender, List<String> args, int index) {
        if (index < args.size()) {
            Player target = Bukkit.getPlayerExact(args.get(index));
            if (target == null) {
                context.message(sender, "environment.player-offline", Map.of("target", args.get(index)));
                return Optional.empty();
            }
            return Optional.of(target);
        }
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        context.message(sender, "environment.console-target-required", Map.of());
        return Optional.empty();
    }

    private Optional<Integer> positiveInt(String input) {
        try {
            int value = Integer.parseInt(input);
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private List<String> timeCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), TIME_VALUES);
        }
        if (ctx.arguments().size() == 2) {
            return worldCompletions(ctx.argument(1));
        }
        return List.of();
    }

    private List<String> shortcutWorldCompletions(CommandContext ctx) {
        return ctx.arguments().size() == 1 ? worldCompletions(ctx.argument(0)) : List.of();
    }

    private List<String> weatherCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), WEATHER_VALUES);
        }
        if (ctx.arguments().size() == 2) {
            java.util.ArrayList<String> values = new java.util.ArrayList<>(DURATION_VALUES);
            values.addAll(worldNames());
            return CommandUtils.matching(ctx.argument(1), values);
        }
        if (ctx.arguments().size() == 3) {
            return worldCompletions(ctx.argument(2));
        }
        return List.of();
    }

    private List<String> weatherShortcutCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            java.util.ArrayList<String> values = new java.util.ArrayList<>(DURATION_VALUES);
            values.addAll(worldNames());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() == 2) {
            return worldCompletions(ctx.argument(1));
        }
        return List.of();
    }

    private List<String> personalTimeCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), PERSONAL_TIME_VALUES);
        }
        if (ctx.arguments().size() == 2) {
            return net.axther.hydroxide.commands.CompletionUtils.onlinePlayers(ctx.argument(1));
        }
        return List.of();
    }

    private List<String> personalWeatherCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            return CommandUtils.matching(ctx.argument(0), PERSONAL_WEATHER_VALUES);
        }
        if (ctx.arguments().size() == 2) {
            return net.axther.hydroxide.commands.CompletionUtils.onlinePlayers(ctx.argument(1));
        }
        return List.of();
    }

    private List<String> worldCompletions(String prefix) {
        return CommandUtils.matching(prefix, worldNames());
    }

    private List<String> worldNames() {
        return Bukkit.getWorlds().stream().map(World::getName).toList();
    }

    private record WeatherArgs(int durationSeconds, int worldIndex) {
    }
}
