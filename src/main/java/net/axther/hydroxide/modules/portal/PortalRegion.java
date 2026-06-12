package net.axther.hydroxide.modules.portal;

public record PortalRegion(
        String name,
        String worldName,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        String destination
) {

    public boolean contains(String worldName, double x, double y, double z) {
        return this.worldName.equalsIgnoreCase(worldName)
                && x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}
