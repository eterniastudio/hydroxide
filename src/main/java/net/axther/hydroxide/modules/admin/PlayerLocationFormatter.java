package net.axther.hydroxide.modules.admin;

final class PlayerLocationFormatter {

    private static final String[] DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private PlayerLocationFormatter() {
    }

    static String compassDirection(float yaw) {
        double normalized = yaw % 360.0D;
        if (normalized < 0.0D) {
            normalized += 360.0D;
        }
        int index = (int) Math.floor((normalized + 22.5D) / 45.0D) % DIRECTIONS.length;
        return DIRECTIONS[index];
    }

    static int depth(int blockY, int seaLevel) {
        return blockY - seaLevel;
    }
}
