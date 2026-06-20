package net.axther.hydroxide.modules.staff;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.modules.welcome.PlayerVisualStateOwner;
import net.axther.hydroxide.modules.welcome.PlayerVisualStateType;
import net.axther.hydroxide.storage.YamlStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VanishModule implements HydroModule, Listener, VanishService {

    private static final List<String> SUBCOMMANDS = List.of("fix", "status");

    private HydroxideContext context;
    private YamlStore vanishStore;
    private VanishSettings settings;
    private VanishStateModel state = VanishStateModel.empty();
    private final Set<UUID> hydroxideInvisibilityPotions = new HashSet<>();

    @Override
    public String id() {
        return "vanish";
    }

    @Override
    public String displayName() {
        return "Vanish";
    }

    @Override
    public String description() {
        return "Staff vanish with tab/visibility hiding, silent joins, and mob target prevention.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.vanishStore = new YamlStore(new File(context.plugin().getDataFolder(), "data/vanish.yml"));
        this.settings = VanishSettings.from(context.plugin().getConfig());
        this.state = loadState();
        context.commands().register("vanish", vanishCommand());
        context.services().vanishService(this);
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        if (!settings.enabled()) {
            context.plugin().getLogger().info("Vanish module is enabled but vanish.enabled=false; visibility hooks are idle.");
        }
        reconcileAllVisibility();
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
        save();
        if (settings == null || settings.restoreVisibilityOnDisable()) {
            restoreAllVisibility();
        }
        context.services().clearVanishService(this);
    }

    @Override
    public void onReload(HydroxideContext context) {
        settings = VanishSettings.from(context.plugin().getConfig());
        state = loadState();
        if (!settings.enabled() || settings.restoreVisibilityOnDisable()) {
            restoreAllVisibility();
        }
        if (settings.enabled()) {
            reconcileAllVisibility();
        }
    }

    private CommandService vanishCommand() {
        return new CommandService(HydroCommand.builder("vanish")
                .permission("hydroxide.command.vanish")
                .usage("/{label} [player|status [player]|fix [player]]")
                .executor(ctx -> vanish(ctx.sender(), ctx.arguments().toArray(String[]::new)))
                .completer(this::vanishCompletions)
                .build(), context.messages());
    }

    private void vanish(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
            status(sender, args);
            return;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("fix")) {
            fix(sender, args);
            return;
        }
        if (settings != null && !settings.enabled()) {
            context.message(sender, "vanish.disabled", Map.of());
            return;
        }

        Player target = resolveTarget(sender, args.length == 0 ? null : args[0]);
        if (target == null) {
            return;
        }
        VanishChange change = state.toggleManual(target.getUniqueId());
        save();
        applyChange(target, change);
        if (change.vanishedNow()) {
            context.message(sender, "vanish.enabled", Map.of("target", target.getName()));
        } else {
            context.message(sender, "vanish.disabled-success", Map.of("target", target.getName()));
        }
    }

    private List<String> vanishCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            java.util.ArrayList<String> suggestions = new java.util.ArrayList<>(SUBCOMMANDS);
            suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(0), suggestions);
        }
        if (ctx.arguments().size() == 2 && (ctx.argument(0).equalsIgnoreCase("status") || ctx.argument(0).equalsIgnoreCase("fix"))) {
            return CompletionUtils.onlinePlayers(ctx.argument(1));
        }
        return List.of();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        if (settings == null || !settings.enabled()) {
            restoreNonVanishedVisualState(joining);
            applyVisibilityState(joining);
            debug(VanishReason.RECONCILE, joining, "vanish disabled on join");
            return;
        }

        VanishJoinResult result = state.applyJoin(joining.getUniqueId(), joining.isOp(), settings);
        if (result.changed()) {
            save();
        }
        if (result.vanished()) {
            event.joinMessage(null);
        } else {
            restoreNonVanishedVisualState(joining);
        }
        debug(result.reason(), joining, "join");
        applyVisibilityState(joining);
        Bukkit.getScheduler().runTask(context.plugin(), this::reconcileAllVisibility);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (settings == null || !settings.enabled()) {
            return;
        }
        if (state.isVanished(event.getPlayer().getUniqueId())) {
            event.quitMessage(null);
        }
        Bukkit.getScheduler().runTask(context.plugin(), this::reconcileAllVisibility);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (settings == null || !settings.enabled()) {
            return;
        }
        if (event.getTarget() instanceof Player player && state.isVanished(player.getUniqueId())) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }

    @Override
    public boolean isVanished(UUID playerId) {
        return settings != null && settings.enabled() && state.isVanished(playerId);
    }

    @Override
    public void reconcileVisibility(Player target) {
        if (target == null || !target.isOnline()) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            applyViewerTarget(viewer, target, VanishReason.RECONCILE);
        }
        reconcileViewer(target);
    }

    @Override
    public void reconcileAllVisibility() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            reconcileViewer(viewer);
        }
    }

    private void status(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args.length > 1 ? args[1] : null);
        if (target == null) {
            return;
        }
        boolean vanished = isVanished(target.getUniqueId());
        context.message(sender, "vanish.status", Map.of(
                "target", target.getName(),
                "state", context.messages().template(vanished ? "vanish.state.vanished" : "vanish.state.visible", vanished ? "vanished" : "visible"),
                "persist", settings != null && settings.persist(),
                "auto_ops", settings != null && settings.autoVanishOps()
        ));
    }

    private void fix(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args.length > 1 ? args[1] : null);
        if (target == null) {
            return;
        }
        VanishChange change = state.fix(target.getUniqueId());
        applyChange(target, change);
        context.message(sender, "vanish.fix.success", Map.of("target", target.getName()));
    }

    private void applyChange(Player target, VanishChange change) {
        if (change.vanishedNow()) {
            context.services().playerVisualStateService()
                    .ifPresent(service -> service.claim(target.getUniqueId(), PlayerVisualStateOwner.VANISH, PlayerVisualStateType.VISIBILITY));
        } else {
            context.services().playerVisualStateService()
                    .ifPresent(service -> service.releaseAll(target.getUniqueId(), PlayerVisualStateOwner.VANISH));
        }
        if (change.restoreVisualState()) {
            restoreNonVanishedVisualState(target);
        }
        if (change.reconcile()) {
            applyVisibilityState(target);
            Bukkit.getScheduler().runTask(context.plugin(), this::reconcileAllVisibility);
        }
        debug(change.reason(), target, change.vanishedNow() ? "vanished" : "visible");
    }

    private void applyVisibilityState(Player target) {
        reconcileVisibility(target);
    }

    private void reconcileViewer(Player viewer) {
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            applyViewerTarget(viewer, candidate, VanishReason.RECONCILE);
        }
    }

    private void applyViewerTarget(Player viewer, Player target, VanishReason reason) {
        boolean targetVanished = settings != null && settings.enabled() && state.isVanished(target.getUniqueId());
        boolean hide = VanishVisibilityPolicy.shouldHide(
                targetVanished,
                viewer.equals(target),
                viewer.hasPermission("hydroxide.vanish.see"),
                settings == null || settings.staffCanSeeVanished()
        );
        if (hide) {
            viewer.hidePlayer(context.plugin(), target);
            debug(reason, target, "hidden from " + viewer.getName());
        } else {
            viewer.showPlayer(context.plugin(), target);
            debug(reason, target, "shown to " + viewer.getName());
        }
    }

    private void restoreNonVanishedVisualState(Player player) {
        context.services().playerVisualStateService().ifPresent(service -> service.restoreIntroState(player));
        player.clearTitle();
        player.sendActionBar(Component.empty());
        if (!isVanished(player.getUniqueId())) {
            boolean entityInvisibilityOwned = context.services().playerVisualStateService()
                    .map(service -> service.hasAnyOwner(player.getUniqueId(), PlayerVisualStateType.ENTITY_INVISIBILITY))
                    .orElse(false);
            if (!entityInvisibilityOwned) {
                player.setInvisible(false);
            }
            if (hydroxideInvisibilityPotions.remove(player.getUniqueId())) {
                boolean potionInvisibilityOwned = context.services().playerVisualStateService()
                        .map(service -> service.hasAnyOwner(player.getUniqueId(), PlayerVisualStateType.POTION_INVISIBILITY))
                        .orElse(false);
                if (!potionInvisibilityOwned) {
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                }
            }
        }
    }

    private Player resolveTarget(CommandSender sender, String name) {
        if (name == null || name.isBlank()) {
            Player self = CommandUtils.playerSender(sender).orElse(null);
            if (self == null) {
                context.message(sender, "vanish.console-target-required", Map.of());
            }
            return self;
        }
        Player target = CommandUtils.onlinePlayer(name).orElse(null);
        if (target == null) {
            context.message(sender, "vanish.player-offline", Map.of("target", name));
        }
        return target;
    }

    private VanishStateModel loadState() {
        YamlConfiguration yaml = vanishStore.load();
        if (settings == null || !settings.enabled()) {
            return VanishStateModel.empty();
        }
        VanishStateModel loaded = VanishStateModel.fromPersisted(yaml.getStringList("vanished"), settings);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (loaded.isVanished(player.getUniqueId())) {
                debug(VanishReason.PERSISTED_RESTORE, player, "loaded from storage");
            }
        }
        return loaded;
    }

    private void save() {
        if (settings != null && (!settings.enabled() || !settings.persist())) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("vanished", state.persistedSnapshot(settings));
        vanishStore.save(yaml);
    }

    private void restoreAllVisibility() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                viewer.showPlayer(context.plugin(), target);
            }
        }
    }

    private void debug(VanishReason reason, Player player, String detail) {
        if (settings != null && settings.debugVisibility()) {
            context.plugin().getLogger().info("[visibility] " + reason.logText() + " for " + player.getName() + ": " + detail);
        }
    }
}
