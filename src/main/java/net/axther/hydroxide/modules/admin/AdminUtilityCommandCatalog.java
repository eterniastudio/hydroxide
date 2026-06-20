package net.axther.hydroxide.modules.admin;

import java.util.List;

final class AdminUtilityCommandCatalog {

    private static final List<String> COMMANDS = List.of(
            "invsee", "invsave", "invcheck", "invload", "invlist", "invremove", "invremoveall",
            "give", "giveall", "donate",
            "endersee", "trash", "workbench", "anvil", "cartography", "smithing", "stonecutter", "loom", "grindstone",
            "clearinventory", "enderchest", "clearender", "condense", "uncondense", "hat", "skull", "suicide", "kill", "killall", "spawnmob", "spawner", "solve", "sound", "shakeitoff", "fireball", "kittycannon", "beezooka", "antioch", "nuke", "extinguish", "burn", "lightning", "exp",
            "groundclean", "remove", "checkexp", "distance", "getpos", "compass", "break", "tree", "bigtree", "launch", "depth",
            "counter", "findbiome", "near", "seen", "lastonline", "whois", "sudo", "sudoall", "staffnote", "note", "alert", "oplist", "checkperm", "haspermission", "checkaccount", "sameip", "lockip", "checkcommand", "ride"
    );

    private AdminUtilityCommandCatalog() {
    }

    static List<String> commands() {
        return COMMANDS;
    }
}
