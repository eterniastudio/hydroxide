package net.axther.hydroxide.modules.builder;

import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Attachable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Hangable;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Snowable;
import org.bukkit.block.data.Waterlogged;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class BlockDataCycler {

    private static final List<BlockFace> FACE_ORDER = List.of(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    );
    private static final List<BlockFace> ROTATION_ORDER = List.of(
            BlockFace.NORTH,
            BlockFace.NORTH_NORTH_EAST,
            BlockFace.NORTH_EAST,
            BlockFace.EAST_NORTH_EAST,
            BlockFace.EAST,
            BlockFace.EAST_SOUTH_EAST,
            BlockFace.SOUTH_EAST,
            BlockFace.SOUTH_SOUTH_EAST,
            BlockFace.SOUTH,
            BlockFace.SOUTH_SOUTH_WEST,
            BlockFace.SOUTH_WEST,
            BlockFace.WEST_SOUTH_WEST,
            BlockFace.WEST,
            BlockFace.WEST_NORTH_WEST,
            BlockFace.NORTH_WEST,
            BlockFace.NORTH_NORTH_WEST
    );

    private BlockDataCycler() {
    }

    static Optional<BlockData> cycle(BlockData original, BlockCyclingCommandParser.Direction direction) {
        BlockData data = original.clone();
        if (data instanceof Directional directional) {
            Optional<BlockFace> next = BlockStateCycler.cycle(orderedFaces(directional.getFaces()), directional.getFacing(), direction);
            if (next.isPresent()) {
                directional.setFacing(next.orElseThrow());
                return Optional.of(data);
            }
        }
        if (data instanceof Rotatable rotatable) {
            Optional<BlockFace> next = BlockStateCycler.cycle(ROTATION_ORDER, rotatable.getRotation(), direction);
            if (next.isPresent()) {
                rotatable.setRotation(next.orElseThrow());
                return Optional.of(data);
            }
        }
        if (data instanceof Orientable orientable) {
            List<Axis> axes = orientable.getAxes().stream()
                    .sorted(Comparator.comparingInt(Enum::ordinal))
                    .toList();
            Optional<Axis> next = BlockStateCycler.cycle(axes, orientable.getAxis(), direction);
            if (next.isPresent()) {
                orientable.setAxis(next.orElseThrow());
                return Optional.of(data);
            }
        }
        if (data instanceof Rail rail) {
            Optional<Rail.Shape> next = BlockStateCycler.cycle(List.copyOf(rail.getShapes()), rail.getShape(), direction);
            if (next.isPresent()) {
                rail.setShape(next.orElseThrow());
                return Optional.of(data);
            }
        }
        if (data instanceof Ageable ageable) {
            ageable.setAge(cycleNumber(ageable.getAge(), 0, ageable.getMaximumAge(), direction));
            return Optional.of(data);
        }
        if (data instanceof AnaloguePowerable analoguePowerable) {
            analoguePowerable.setPower(cycleNumber(analoguePowerable.getPower(), 0, analoguePowerable.getMaximumPower(), direction));
            return Optional.of(data);
        }
        if (data instanceof Levelled levelled) {
            levelled.setLevel(cycleNumber(levelled.getLevel(), 0, levelled.getMaximumLevel(), direction));
            return Optional.of(data);
        }
        if (data instanceof Bisected bisected) {
            Optional<Bisected.Half> next = BlockStateCycler.cycle(List.of(Bisected.Half.BOTTOM, Bisected.Half.TOP), bisected.getHalf(), direction);
            if (next.isPresent()) {
                bisected.setHalf(next.orElseThrow());
                return Optional.of(data);
            }
        }
        if (data instanceof FaceAttachable faceAttachable) {
            Optional<FaceAttachable.AttachedFace> next = BlockStateCycler.cycle(
                    List.of(FaceAttachable.AttachedFace.FLOOR, FaceAttachable.AttachedFace.WALL, FaceAttachable.AttachedFace.CEILING),
                    faceAttachable.getAttachedFace(),
                    direction
            );
            if (next.isPresent() && trySetAttachedFace(faceAttachable, next.orElseThrow())) {
                return Optional.of(data);
            }
        }
        if (data instanceof Openable openable) {
            openable.setOpen(!openable.isOpen());
            return Optional.of(data);
        }
        if (data instanceof Powerable powerable) {
            powerable.setPowered(!powerable.isPowered());
            return Optional.of(data);
        }
        if (data instanceof Lightable lightable) {
            lightable.setLit(!lightable.isLit());
            return Optional.of(data);
        }
        if (data instanceof Waterlogged waterlogged) {
            waterlogged.setWaterlogged(!waterlogged.isWaterlogged());
            return Optional.of(data);
        }
        if (data instanceof Snowable snowable) {
            snowable.setSnowy(!snowable.isSnowy());
            return Optional.of(data);
        }
        if (data instanceof Hangable hangable) {
            hangable.setHanging(!hangable.isHanging());
            return Optional.of(data);
        }
        if (data instanceof Attachable attachable) {
            attachable.setAttached(!attachable.isAttached());
            return Optional.of(data);
        }
        return Optional.empty();
    }

    private static boolean trySetAttachedFace(FaceAttachable faceAttachable, FaceAttachable.AttachedFace face) {
        try {
            faceAttachable.setAttachedFace(face);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static int cycleNumber(int current, int min, int max, BlockCyclingCommandParser.Direction direction) {
        if (max <= min) {
            return current;
        }
        if (direction == BlockCyclingCommandParser.Direction.FORWARD) {
            return current >= max ? min : current + 1;
        }
        return current <= min ? max : current - 1;
    }

    private static List<BlockFace> orderedFaces(Iterable<BlockFace> faces) {
        List<BlockFace> available = new ArrayList<>();
        faces.forEach(available::add);
        List<BlockFace> ordered = new ArrayList<>();
        for (BlockFace face : FACE_ORDER) {
            if (available.contains(face)) {
                ordered.add(face);
            }
        }
        available.stream()
                .filter(face -> !ordered.contains(face))
                .sorted(Comparator.comparing(Enum::name))
                .forEach(ordered::add);
        return ordered;
    }
}
