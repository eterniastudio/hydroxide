package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUtilityCommandCatalogTest {

    @Test
    void includesModernVirtualWorkstationCommands() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("loom"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("grindstone"));
    }

    @Test
    void includesInventoryCondenseCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("condense"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("uncondense"));
    }

    @Test
    void includesExperienceInspectionCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("checkexp"));
    }

    @Test
    void includesDistanceCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("distance"));
    }

    @Test
    void includesKillAllCleanupCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("killall"));
    }

    @Test
    void includesGroundEntityCleanupCommands() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("groundclean"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("remove"));
    }

    @Test
    void includesInventorySnapshotCommands() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("invsave"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("invcheck"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("invload"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("invlist"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("invremove"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("invremoveall"));
    }

    @Test
    void includesItemDistributionCommands() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("give"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("giveall"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("donate"));
    }

    @Test
    void includesPermissionDiagnosticCommands() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("checkperm"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("haspermission"));
    }

    @Test
    void includesAccountDiagnosticCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("checkaccount"));
    }

    @Test
    void includesSameIpDiagnosticCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("sameip"));
    }

    @Test
    void includesCommandDiagnosticCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("checkcommand"));
    }

    @Test
    void includesLoginAlertCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("alert"));
    }

    @Test
    void includesOperatorListCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("oplist"));
    }

    @Test
    void includesSudoAllCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("sudoall"));
    }

    @Test
    void includesCounterCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("counter"));
    }

    @Test
    void includesSpawnMobCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("spawnmob"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("spawner"));
    }

    @Test
    void includesSolveCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("solve"));
    }

    @Test
    void includesSoundCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("sound"));
    }

    @Test
    void includesShakeItOffCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("shakeitoff"));
    }

    @Test
    void includesRideCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("ride"));
    }

    @Test
    void includesFireballCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("fireball"));
    }

    @Test
    void includesAnimalCannonCommands() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("kittycannon"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("beezooka"));
    }

    @Test
    void includesAntiochCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("antioch"));
    }

    @Test
    void includesNukeCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("nuke"));
    }

    @Test
    void includesBreakCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("break"));
    }

    @Test
    void includesTreeCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("tree"));
        assertTrue(AdminUtilityCommandCatalog.commands().contains("bigtree"));
    }

    @Test
    void includesLaunchCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("launch"));
    }

    @Test
    void includesFindBiomeCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("findbiome"));
    }

    @Test
    void includesLastOnlineCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("lastonline"));
    }

    @Test
    void includesCmiStyleNoteAliasCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("note"));
    }

    @Test
    void includesCmiStyleLockIpCommand() {
        assertTrue(AdminUtilityCommandCatalog.commands().contains("lockip"));
    }
}
