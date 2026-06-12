package net.axther.hydroxide.modules.armorstand;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ArmorStandEditorModule implements HydroModule, Listener, CommandExecutor, TabCompleter {

    private static final List<String> ACTIONS = List.of(
            "arms", "baseplate", "copy", "equip", "glowing", "gravity", "invulnerable",
            "lock", "marker", "move", "paste", "pose", "rotate", "small", "unlock", "visible"
    );

    private HydroxideContext context;
    private YamlStore store;
    private final Map<UUID, ArmorStandSnapshot> clipboards = new HashMap<>();

    @Override
    public String id() {
        return "armor-stands";
    }

    @Override
    public String displayName() {
        return "Armor Stand Editor";
    }

    @Override
    public String description() {
        return "Command-driven armor stand editor with copy/paste, equipment, pose, and persistent locks.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        this.context = context;
        this.store = new YamlStore(new File(context.plugin().getDataFolder(), "armorstands.yml"));
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        context.commands().register("armorstand", this);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtils.playerSender(sender).orElse(null);
        if (player == null) {
            context.send(sender, "<red>Only players can edit armor stands.");
            return true;
        }
        if (!context.requirePermission(sender, "hydroxide.armorstand.edit")) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " " + String.join("|", ACTIONS));
            return true;
        }
        ArmorStand stand = target(player);
        if (stand == null) {
            context.send(player, "<red>Look at an armor stand within 8 blocks.");
            return true;
        }
        if (!canEdit(player, stand)) {
            context.send(player, "<red>That armor stand is locked.");
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "arms" -> stand.setArms(!stand.hasArms());
            case "baseplate" -> stand.setBasePlate(!stand.hasBasePlate());
            case "gravity" -> stand.setGravity(!stand.hasGravity());
            case "visible" -> stand.setVisible(!stand.isVisible());
            case "glowing" -> stand.setGlowing(!stand.isGlowing());
            case "small" -> stand.setSmall(!stand.isSmall());
            case "marker" -> stand.setMarker(!stand.isMarker());
            case "invulnerable" -> stand.setInvulnerable(!stand.isInvulnerable());
            case "rotate" -> stand.setRotation(args.length > 1 ? parseFloat(args[1], stand.getLocation().getYaw()) : stand.getLocation().getYaw() + 15.0f, stand.getLocation().getPitch());
            case "move" -> move(stand, args);
            case "pose" -> pose(stand, args);
            case "equip" -> equip(player, stand, args);
            case "copy" -> clipboards.put(player.getUniqueId(), ArmorStandSnapshot.from(stand));
            case "paste" -> {
                ArmorStandSnapshot snapshot = clipboards.get(player.getUniqueId());
                if (snapshot == null) {
                    context.send(player, "<red>Your armor stand clipboard is empty.");
                    return true;
                }
                snapshot.apply(stand);
            }
            case "lock" -> lock(player, stand);
            case "unlock" -> unlock(stand);
            default -> {
                context.send(player, "<red>Usage: /" + label + " " + String.join("|", ACTIONS));
                return true;
            }
        }
        context.send(player, "<green>Armor stand updated.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtils.matching(args[0], ACTIONS);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("equip")) {
            return CommandUtils.matching(args[1], List.of("chest", "feet", "hand", "head", "legs", "off_hand"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pose")) {
            return CommandUtils.matching(args[1], List.of("body", "head", "left_arm", "left_leg", "right_arm", "right_leg"));
        }
        return List.of();
    }

    @EventHandler(ignoreCancelled = true)
    public void onManipulate(PlayerArmorStandManipulateEvent event) {
        if (!canEdit(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
            context.send(event.getPlayer(), "<red>That armor stand is locked.");
        }
    }

    private ArmorStand target(Player player) {
        Entity target = player.getTargetEntity(8);
        return target instanceof ArmorStand armorStand ? armorStand : null;
    }

    private boolean canEdit(Player player, ArmorStand stand) {
        return ArmorStandLockPolicy.canEdit(lock(stand).orElse(null), player.getUniqueId(), player.hasPermission("hydroxide.armorstand.bypass"));
    }

    private void move(ArmorStand stand, String[] args) {
        double dx = args.length > 1 ? parseDouble(args[1], 0.0D) : 0.0D;
        double dy = args.length > 2 ? parseDouble(args[2], 0.0D) : 0.0D;
        double dz = args.length > 3 ? parseDouble(args[3], 0.0D) : 0.0D;
        stand.teleport(stand.getLocation().add(dx, dy, dz));
    }

    private void pose(ArmorStand stand, String[] args) {
        if (args.length < 5) {
            return;
        }
        EulerAngle angle = new EulerAngle(Math.toRadians(parseDouble(args[2], 0.0D)), Math.toRadians(parseDouble(args[3], 0.0D)), Math.toRadians(parseDouble(args[4], 0.0D)));
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "head" -> stand.setHeadPose(angle);
            case "body" -> stand.setBodyPose(angle);
            case "left_arm" -> stand.setLeftArmPose(angle);
            case "right_arm" -> stand.setRightArmPose(angle);
            case "left_leg" -> stand.setLeftLegPose(angle);
            case "right_leg" -> stand.setRightLegPose(angle);
            default -> {
            }
        }
    }

    private void equip(Player player, ArmorStand stand, String[] args) {
        if (args.length < 2) {
            return;
        }
        EquipmentSlot slot = switch (args[1].toLowerCase(Locale.ROOT)) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            case "off_hand" -> EquipmentSlot.OFF_HAND;
            default -> EquipmentSlot.HAND;
        };
        stand.setItem(slot, player.getInventory().getItemInMainHand().clone());
    }

    private void lock(Player player, ArmorStand stand) {
        YamlConfiguration yaml = store.load();
        String path = path(stand.getUniqueId());
        Location location = stand.getLocation();
        yaml.set(path + ".owner", player.getUniqueId().toString());
        yaml.set(path + ".world", location.getWorld().getName());
        yaml.set(path + ".x", location.getBlockX());
        yaml.set(path + ".y", location.getBlockY());
        yaml.set(path + ".z", location.getBlockZ());
        store.save(yaml);
    }

    private void unlock(ArmorStand stand) {
        YamlConfiguration yaml = store.load();
        yaml.set(path(stand.getUniqueId()), null);
        store.save(yaml);
    }

    private Optional<ArmorStandLock> lock(ArmorStand stand) {
        ConfigurationSection section = store.load().getConfigurationSection(path(stand.getUniqueId()));
        if (section == null) {
            return Optional.empty();
        }
        return Optional.of(new ArmorStandLock(
                stand.getUniqueId(),
                UUID.fromString(section.getString("owner")),
                section.getString("world", stand.getWorld().getName()),
                section.getInt("x"),
                section.getInt("y"),
                section.getInt("z")
        ));
    }

    private String path(UUID standId) {
        return "locked." + standId;
    }

    private float parseFloat(String input, float fallback) {
        try {
            return Float.parseFloat(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private double parseDouble(String input, double fallback) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private record ArmorStandSnapshot(boolean arms, boolean basePlate, boolean gravity, boolean visible,
                                      boolean glowing, boolean small, boolean marker, boolean invulnerable,
                                      EulerAngle head, EulerAngle body, EulerAngle leftArm, EulerAngle rightArm,
                                      EulerAngle leftLeg, EulerAngle rightLeg) {
        static ArmorStandSnapshot from(ArmorStand stand) {
            return new ArmorStandSnapshot(
                    stand.hasArms(),
                    stand.hasBasePlate(),
                    stand.hasGravity(),
                    stand.isVisible(),
                    stand.isGlowing(),
                    stand.isSmall(),
                    stand.isMarker(),
                    stand.isInvulnerable(),
                    stand.getHeadPose(),
                    stand.getBodyPose(),
                    stand.getLeftArmPose(),
                    stand.getRightArmPose(),
                    stand.getLeftLegPose(),
                    stand.getRightLegPose()
            );
        }

        void apply(ArmorStand stand) {
            stand.setArms(arms);
            stand.setBasePlate(basePlate);
            stand.setGravity(gravity);
            stand.setVisible(visible);
            stand.setGlowing(glowing);
            stand.setSmall(small);
            stand.setMarker(marker);
            stand.setInvulnerable(invulnerable);
            stand.setHeadPose(head);
            stand.setBodyPose(body);
            stand.setLeftArmPose(leftArm);
            stand.setRightArmPose(rightArm);
            stand.setLeftLegPose(leftLeg);
            stand.setRightLegPose(rightLeg);
        }
    }
}
