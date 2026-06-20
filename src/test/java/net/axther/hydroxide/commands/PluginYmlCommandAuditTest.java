package net.axther.hydroxide.commands;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginYmlCommandAuditTest {

    @Test
    void vanishUsageDocumentsRepairAndStatusSubcommands() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        String usage = pluginYml.getString("commands.vanish.usage", "");

        assertTrue(usage.contains("fix"), "vanish usage should document /vanish fix [player]");
        assertTrue(usage.contains("status"), "vanish usage should document /vanish status [player]");
    }

    @Test
    void serverInfoCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.help"));
        assertTrue(pluginYml.getStringList("commands.help.aliases").contains("ehelp"));
        assertTrue(pluginYml.getString("commands.help.usage", "").contains("query"));
        assertTrue(pluginYml.isConfigurationSection("commands.editlocale"));
        assertTrue(pluginYml.getStringList("commands.editlocale.aliases").contains("localeedit"));
        assertTrue(pluginYml.getString("commands.editlocale.usage", "").contains("set"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.editlocale"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.disabled.bypass"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.permission.bypass"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.cost.bypass"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.warmup.bypass"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.cooldown.bypass"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.cooldown.manage"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.world-restriction.bypass"));
        assertTrue(pluginYml.isConfigurationSection("commands.motd"));
        assertTrue(pluginYml.isConfigurationSection("commands.info"));
        assertTrue(pluginYml.getString("commands.info.permission", "").contains("info"));
        assertTrue(pluginYml.isConfigurationSection("commands.rules"));
        assertTrue(pluginYml.isConfigurationSection("commands.ctext"));
        assertTrue(pluginYml.getStringList("commands.ctext.aliases").contains("customtext"));
        assertTrue(pluginYml.getString("commands.ctext.usage", "").contains("sourcePlayer"));
        assertTrue(pluginYml.isConfigurationSection("commands.editctext"));
        assertTrue(pluginYml.getStringList("commands.editctext.aliases").contains("ctextedit"));
        assertTrue(pluginYml.getString("commands.editctext.usage", "").contains("set"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.editctext"));
        assertTrue(pluginYml.isConfigurationSection("commands.maintenance"));
        assertTrue(pluginYml.getString("commands.maintenance.usage", "").contains("true"));
        assertTrue(pluginYml.isConfigurationSection("commands.helpop"));
        assertTrue(pluginYml.isString("commands.helpop.usage"));
        assertTrue(pluginYml.isConfigurationSection("commands.usermeta"));
        assertTrue(pluginYml.getString("commands.usermeta.usage", "").contains("increment"));
        assertTrue(pluginYml.isConfigurationSection("commands.list"));
        assertTrue(pluginYml.getStringList("commands.list.aliases").contains("who"));
        assertTrue(pluginYml.isConfigurationSection("commands.ping"));
        assertTrue(pluginYml.isConfigurationSection("commands.gc"));
        assertTrue(pluginYml.getStringList("commands.gc.aliases").contains("lag"));
        assertTrue(pluginYml.isConfigurationSection("commands.tps"));
        assertTrue(pluginYml.getString("commands.tps.usage", "").contains("-spikes"));
        assertTrue(pluginYml.isConfigurationSection("commands.servertime"));
        assertTrue(pluginYml.getString("commands.servertime.permission", "").contains("servertime"));
        assertTrue(pluginYml.isConfigurationSection("commands.setmotd"));
        assertTrue(pluginYml.getString("commands.setmotd.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.status"));
        assertTrue(pluginYml.getStringList("commands.status.aliases").contains("serverstatus"));
        assertTrue(pluginYml.isConfigurationSection("commands.maxplayers"));
        assertTrue(pluginYml.getString("commands.maxplayers.usage", "").contains("amount"));
    }

    @Test
    void economyCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.balance"));
        assertTrue(pluginYml.isConfigurationSection("commands.pay"));
        assertTrue(pluginYml.isConfigurationSection("commands.paytoggle"));
        assertTrue(pluginYml.getStringList("commands.paytoggle.aliases").contains("ptoggle"));
        assertTrue(pluginYml.isConfigurationSection("commands.cheque"));
        assertTrue(pluginYml.getStringList("commands.cheque.aliases").contains("check"));
        assertTrue(pluginYml.isConfigurationSection("commands.eco"));
        assertTrue(pluginYml.isConfigurationSection("commands.baltop"));
    }

    @Test
    void nicknameCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.nickname"));
        assertTrue(pluginYml.getString("commands.nickname.usage", "").contains("clear"));
        assertTrue(pluginYml.isConfigurationSection("commands.realname"));
        assertTrue(pluginYml.isConfigurationSection("commands.nameplate"));
        assertTrue(pluginYml.getString("commands.nameplate.usage", "").contains("-pref:"));
        assertTrue(pluginYml.getString("commands.nameplate.usage", "").contains("-suf:"));
        assertTrue(pluginYml.getString("commands.nameplate.usage", "").contains("-c:"));
        assertTrue(pluginYml.getString("commands.nameplate.usage", "").contains("-s"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.nameplate"));
    }

    @Test
    void environmentCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.time"));
        assertTrue(pluginYml.isConfigurationSection("commands.day"));
        assertTrue(pluginYml.isConfigurationSection("commands.night"));
        assertTrue(pluginYml.isConfigurationSection("commands.weather"));
        assertTrue(pluginYml.isConfigurationSection("commands.sun"));
        assertTrue(pluginYml.isConfigurationSection("commands.storm"));
        assertTrue(pluginYml.isConfigurationSection("commands.thunder"));
        assertTrue(pluginYml.isConfigurationSection("commands.ptime"));
        assertTrue(pluginYml.getStringList("commands.ptime.aliases").contains("playertime"));
        assertTrue(pluginYml.isConfigurationSection("commands.pweather"));
        assertTrue(pluginYml.getStringList("commands.pweather.aliases").contains("playerweather"));
    }

    @Test
    void directTeleportCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.tpahere"));
        assertTrue(pluginYml.getStringList("commands.tpahere.aliases").contains("tpaskhere"));
        assertTrue(pluginYml.isConfigurationSection("commands.tpaall"));
        assertTrue(pluginYml.isString("commands.tpaall.usage"));
        assertTrue(pluginYml.isConfigurationSection("commands.tpacancel"));
        assertTrue(pluginYml.isConfigurationSection("commands.tptoggle"));
        assertTrue(pluginYml.getStringList("commands.tptoggle.aliases").contains("tpatoggle"));
        assertTrue(pluginYml.isConfigurationSection("commands.tpauto"));
        assertTrue(pluginYml.getStringList("commands.tpauto.aliases").contains("tpautoaccept"));
        assertTrue(pluginYml.isConfigurationSection("commands.tp"));
        assertTrue(pluginYml.isConfigurationSection("commands.tphere"));
        assertTrue(pluginYml.getStringList("commands.tphere.aliases").contains("s"));
        assertTrue(pluginYml.isConfigurationSection("commands.tpo"));
        assertTrue(pluginYml.getString("commands.tpo.usage", "").contains("target"));
        assertTrue(pluginYml.isConfigurationSection("commands.tpohere"));
        assertTrue(pluginYml.getString("commands.tpohere.usage", "").contains("player"));
        assertTrue(pluginYml.isConfigurationSection("commands.tpall"));
        assertTrue(pluginYml.isConfigurationSection("commands.tpallworld"));
        assertTrue(pluginYml.getString("commands.tpallworld.usage", "").contains("world;x;y;z"));
        assertTrue(pluginYml.isConfigurationSection("commands.tppos"));
        assertTrue(pluginYml.getString("commands.tppos.usage", "").contains("-p:"));
        assertTrue(pluginYml.isConfigurationSection("commands.tpopos"));
        assertTrue(pluginYml.getString("commands.tpopos.usage", "").contains("pitch"));
        assertTrue(pluginYml.isConfigurationSection("commands.dback"));
        assertTrue(pluginYml.getString("commands.dback.usage", "").contains("player"));
        assertTrue(pluginYml.isConfigurationSection("commands.resetback"));
        assertTrue(pluginYml.getString("commands.resetback.usage", "").contains("-s"));
        assertTrue(pluginYml.getString("commands.resetback.permission", "").contains("resetback"));
        assertTrue(pluginYml.isConfigurationSection("commands.jump"));
        assertTrue(pluginYml.getStringList("commands.jump.aliases").contains("j"));
        assertTrue(pluginYml.isConfigurationSection("commands.down"));
        assertTrue(pluginYml.getString("commands.down.usage", "").contains("max"));
        assertTrue(pluginYml.getStringList("commands.down.aliases").contains("descend"));
        assertTrue(pluginYml.isConfigurationSection("commands.world"));
        assertTrue(pluginYml.getString("commands.world.usage", "").contains("normal"));
        assertTrue(pluginYml.getString("commands.world.usage", "").contains("player"));
        assertTrue(pluginYml.isConfigurationSection("commands.patrol"));
        assertTrue(pluginYml.getString("commands.patrol.usage", "").contains("reset"));
        assertTrue(pluginYml.getString("commands.patrol.permission", "").contains("patrol"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.resetback"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.patrol"));
    }

    @Test
    void disciplineCommandsAreDeclaredForPaperRegistration() throws Exception {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));
        String pluginText = Files.readString(Path.of("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.kick"));
        assertTrue(pluginYml.isString("commands.kick.usage"));
        assertTrue(pluginYml.getString("commands.kick.usage", "").contains("all"));
        assertTrue(pluginYml.getString("commands.kick.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.kickall"));
        assertTrue(pluginYml.getString("commands.kickall.usage", "").contains("reason"));
        assertTrue(pluginYml.getString("commands.kickall.usage", "").contains("-s"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.kick.bypass"));
        assertTrue(pluginYml.isConfigurationSection("commands.ban"));
        assertTrue(pluginYml.isConfigurationSection("commands.banlist"));
        assertTrue(pluginYml.isString("commands.banlist.usage"));
        assertTrue(pluginYml.isConfigurationSection("commands.checkban"));
        assertTrue(pluginYml.isString("commands.checkban.usage"));
        assertTrue(pluginYml.isConfigurationSection("commands.tempban"));
        assertTrue(pluginYml.isConfigurationSection("commands.unban"));
        assertTrue(pluginYml.getStringList("commands.unban.aliases").contains("pardon"));
        assertTrue(pluginYml.isConfigurationSection("commands.ipban"));
        assertTrue(pluginYml.getStringList("commands.ipban.aliases").contains("banip"));
        assertTrue(pluginYml.getStringList("commands.ipban.aliases").contains("ban-ip"));
        assertTrue(pluginYml.getString("commands.ipban.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.ipbanlist"));
        assertTrue(pluginYml.isConfigurationSection("commands.unbanip"));
        assertTrue(pluginYml.getStringList("commands.unbanip.aliases").contains("pardonip"));
        assertTrue(pluginYml.getStringList("commands.unbanip.aliases").contains("pardon-ip"));
        assertTrue(pluginText.contains("hydroxide.command.ipban: true"));
        assertTrue(pluginText.contains("hydroxide.command.ipban.bypass: true"));
        assertTrue(pluginText.contains("hydroxide.command.ipbanlist: true"));
        assertTrue(pluginText.contains("hydroxide.command.unbanip: true"));
        assertTrue(pluginYml.isConfigurationSection("commands.mute"));
        assertTrue(pluginYml.getStringList("commands.mute.aliases").contains("silence"));
        assertTrue(pluginYml.isConfigurationSection("commands.mutechat"));
        assertTrue(pluginYml.getStringList("commands.mutechat.aliases").contains("globalmute"));
        assertTrue(pluginYml.getString("commands.mutechat.usage", "").contains("-s"));
        assertTrue(pluginText.contains("hydroxide.command.mutechat: true"));
        assertTrue(pluginText.contains("hydroxide.command.mutechat.bypass: true"));
        assertTrue(pluginYml.isConfigurationSection("commands.tempmute"));
        assertTrue(pluginYml.isConfigurationSection("commands.unmute"));
        assertTrue(pluginYml.isConfigurationSection("commands.warn"));
        assertTrue(pluginYml.isConfigurationSection("commands.warnings"));
        assertTrue(pluginYml.getStringList("commands.warnings.aliases").contains("warns"));
        assertTrue(pluginYml.isConfigurationSection("commands.clearwarnings"));
        assertTrue(pluginYml.isConfigurationSection("commands.editwarnings"));
        assertTrue(pluginYml.getString("commands.editwarnings.usage", "").contains("clearall"));
        assertTrue(pluginText.contains("hydroxide.command.editwarnings: true"));
    }

    @Test
    void moderationQualityOfLifeAliasesAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));
        String pluginText = assertDoesNotThrow(() -> Files.readString(Path.of("src/main/resources/plugin.yml")));

        assertTrue(pluginYml.getStringList("commands.gamemode.aliases").contains("gm"));
        assertTrue(pluginYml.getStringList("commands.gamemode.aliases").contains("gmc"));
        assertTrue(pluginYml.getStringList("commands.gamemode.aliases").contains("gms"));
        assertTrue(pluginYml.getStringList("commands.gamemode.aliases").contains("gma"));
        assertTrue(pluginYml.getStringList("commands.gamemode.aliases").contains("gmsp"));
        assertTrue(pluginYml.getStringList("commands.speed.aliases").contains("flyspeed"));
        assertTrue(pluginYml.getStringList("commands.speed.aliases").contains("walkspeed"));
        assertTrue(pluginYml.isConfigurationSection("commands.tfly"));
        assertTrue(pluginYml.getString("commands.tfly.usage", "").contains("timeInSec"));
        assertTrue(pluginYml.isConfigurationSection("commands.tgod"));
        assertTrue(pluginYml.getString("commands.tgod.usage", "").contains("timeInSec"));
        assertTrue(pluginYml.getString("commands.heal.usage", "").contains("all"));
        assertTrue(pluginYml.getString("commands.heal.usage", "").contains("percent"));
        assertTrue(pluginYml.getString("commands.feed.usage", "").contains("all"));
        assertTrue(pluginYml.getString("commands.feed.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.effect"));
        assertTrue(pluginYml.getStringList("commands.effect.aliases").contains("potion"));
        assertTrue(pluginYml.isConfigurationSection("commands.air"));
        assertTrue(pluginYml.isConfigurationSection("commands.hunger"));
        assertTrue(pluginYml.isString("commands.hunger.usage"));
        assertTrue(pluginYml.isConfigurationSection("commands.saturation"));
        assertTrue(pluginYml.isString("commands.saturation.usage"));
        assertTrue(pluginYml.isConfigurationSection("commands.maxhp"));
        assertTrue(pluginYml.getString("commands.maxhp.usage", "").contains("clear"));
        assertTrue(pluginYml.isConfigurationSection("commands.scale"));
        assertTrue(pluginYml.getString("commands.scale.usage", "").contains("clear"));
        assertTrue(pluginYml.getString("commands.scale.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.glow"));
        assertTrue(pluginYml.getString("commands.glow.usage", "").contains("color"));
        assertTrue(pluginYml.getString("commands.glow.permission", "").contains("glow"));
        assertTrue(pluginText.contains("hydroxide.command.glow: true"));
        assertTrue(pluginText.contains("hydroxide.command.glow.others: true"));
        assertTrue(pluginYml.isConfigurationSection("commands.notarget"));
        assertTrue(pluginYml.getString("commands.notarget.usage", "").contains("true"));
        assertTrue(pluginYml.isConfigurationSection("commands.playercollision"));
        assertTrue(pluginYml.getString("commands.playercollision.usage", "").contains("-s"));
        assertTrue(pluginYml.getString("commands.playercollision.permission", "").contains("playercollision"));
        assertTrue(pluginYml.isConfigurationSection("commands.cuff"));
        assertTrue(pluginYml.getString("commands.cuff.usage", "").contains("true"));
        assertTrue(pluginYml.getStringList("commands.cuff.aliases").contains("handcuff"));
        assertTrue(pluginYml.isConfigurationSection("commands.falldistance"));
        assertTrue(pluginYml.getStringList("commands.falldistance.aliases").contains("falldist"));
    }

    @Test
    void afkCommandIsDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.afk"));
        assertTrue(pluginYml.isString("commands.afk.usage"));
        assertTrue(pluginYml.isConfigurationSection("commands.afkcheck"));
        assertTrue(pluginYml.isString("commands.afkcheck.usage"));
    }

    @Test
    void mailCommandIsDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.mail"));
        assertTrue(pluginYml.getStringList("commands.mail.aliases").contains("email"));
        assertTrue(pluginYml.isString("commands.mail.usage"));
        assertTrue(pluginYml.getString("commands.mail.usage", "").contains("sendtemp"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.mail.sendtemp"));
        assertTrue(pluginYml.isConfigurationSection("commands.mailall"));
        assertTrue(pluginYml.getString("commands.mailall.usage", "").contains("remove"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.mailall"));
    }

    @Test
    void channelCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.channel"));
        assertTrue(pluginYml.isConfigurationSection("commands.g"));
        assertTrue(pluginYml.isConfigurationSection("commands.l"));
        assertTrue(pluginYml.isConfigurationSection("commands.sc"));
        assertTrue(pluginYml.isConfigurationSection("commands.staffmsg"));
        assertTrue(pluginYml.getStringList("commands.staffmsg.aliases").contains("staffchat"));
        assertTrue(pluginYml.getStringList("commands.staffmsg.aliases").contains("schat"));
        assertTrue(pluginYml.getString("commands.staffmsg.permission", "").contains("hydroxide.channel.staff"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.channel.staff"));
        assertTrue(pluginYml.isConfigurationSection("commands.trade"));
    }

    @Test
    void jailCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.setjail"));
        assertTrue(pluginYml.isConfigurationSection("commands.jail"));
        assertTrue(pluginYml.getString("commands.jail.usage", "").contains("r:<reason>"));
        assertTrue(pluginYml.getString("commands.jail.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.togglejail"));
        assertTrue(pluginYml.getStringList("commands.togglejail.aliases").contains("etogglejail"));
        assertTrue(pluginYml.getString("commands.togglejail.usage", "").contains("r:<reason>"));
        assertTrue(pluginYml.isConfigurationSection("commands.unjail"));
        assertTrue(pluginYml.isConfigurationSection("commands.jails"));
        assertTrue(pluginYml.getStringList("commands.jails.aliases").contains("jaillist"));
        assertTrue(pluginYml.getString("commands.jails.usage", "").contains("[jail]"));
        assertTrue(pluginYml.getString("commands.jails.usage", "").contains("[cellId]"));
        assertTrue(pluginYml.isConfigurationSection("commands.deljail"));
        assertTrue(pluginYml.getStringList("commands.deljail.aliases").contains("rmjail"));
        assertTrue(pluginYml.getStringList("commands.deljail.aliases").contains("remjail"));
    }

    @Test
    void chatControlCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.broadcast"));
        assertTrue(pluginYml.getString("commands.broadcast.usage", "").contains("-w:"));
        assertTrue(pluginYml.getString("commands.broadcast.usage", "").contains("-r:"));
        assertTrue(pluginYml.getString("commands.broadcast.usage", "").contains("-c:"));
        assertTrue(pluginYml.isConfigurationSection("commands.me"));
        assertTrue(pluginYml.isConfigurationSection("commands.clearchat"));
        assertTrue(pluginYml.getStringList("commands.clearchat.aliases").contains("cc"));
        assertTrue(pluginYml.getString("commands.clearchat.usage", "").contains("self"));
        assertTrue(pluginYml.getString("commands.clearchat.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.actionbarmsg"));
        assertTrue(pluginYml.getStringList("commands.actionbarmsg.aliases").contains("actionbar"));
        assertTrue(pluginYml.isConfigurationSection("commands.bossbarmsg"));
        assertTrue(pluginYml.getStringList("commands.bossbarmsg.aliases").contains("bbroadcast"));
        assertTrue(pluginYml.isConfigurationSection("commands.titlemsg"));
        assertTrue(pluginYml.getStringList("commands.titlemsg.aliases").contains("tbroadcast"));
        assertTrue(pluginYml.isConfigurationSection("commands.ctellraw"));
        assertTrue(pluginYml.getString("commands.ctellraw.usage", "").contains("player|all"));
        assertTrue(pluginYml.getStringList("commands.ctellraw.aliases").contains("tellrawmsg"));
        assertTrue(pluginYml.isConfigurationSection("commands.ignore"));
        assertTrue(pluginYml.getStringList("commands.ignore.aliases").contains("unignore"));
        assertTrue(pluginYml.isConfigurationSection("commands.chat"));
        assertTrue(pluginYml.getString("commands.chat.usage", "").contains("off"));
        assertTrue(pluginYml.isConfigurationSection("commands.msgtoggle"));
        assertTrue(pluginYml.getStringList("commands.msgtoggle.aliases").contains("pmtoggle"));
        assertTrue(pluginYml.isConfigurationSection("commands.socialspy"));
        assertTrue(pluginYml.getStringList("commands.socialspy.aliases").contains("msgspy"));
        assertTrue(pluginYml.isConfigurationSection("commands.commandspy"));
        assertTrue(pluginYml.getStringList("commands.commandspy.aliases").contains("cmdspy"));
        assertTrue(pluginYml.isConfigurationSection("commands.chatcolor"));
        assertTrue(pluginYml.getStringList("commands.chatcolor.aliases").contains("chatcolour"));
        assertTrue(pluginYml.isConfigurationSection("commands.colors"));
        assertTrue(pluginYml.isConfigurationSection("commands.colorpicker"));
        assertTrue(pluginYml.isConfigurationSection("commands.colorlimits"));
        assertTrue(pluginYml.getString("commands.colorpicker.usage", "").contains("hex"));
    }

    @Test
    void utilityParityCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));
        String pluginText = assertDoesNotThrow(() -> Files.readString(Path.of("src/main/resources/plugin.yml")));

        assertTrue(pluginYml.isConfigurationSection("commands.invsave"));
        assertTrue(pluginYml.isConfigurationSection("commands.give"));
        assertTrue(pluginYml.getString("commands.give.usage", "").contains("item"));
        assertTrue(pluginYml.isConfigurationSection("commands.giveall"));
        assertTrue(pluginYml.getString("commands.giveall.usage", "").contains("item"));
        assertTrue(pluginYml.isConfigurationSection("commands.donate"));
        assertTrue(pluginYml.getString("commands.donate.usage", "").contains("amount"));
        assertTrue(pluginYml.isConfigurationSection("commands.checkperm"));
        assertTrue(pluginYml.getString("commands.checkperm.usage", "").contains("keyword"));
        assertTrue(pluginYml.isConfigurationSection("commands.haspermission"));
        assertTrue(pluginYml.getString("commands.haspermission.usage", "").contains("permission"));
        assertTrue(pluginYml.isConfigurationSection("commands.checkaccount"));
        assertTrue(pluginYml.getString("commands.checkaccount.usage", "").contains("player"));
        assertTrue(pluginYml.isConfigurationSection("commands.checkcommand"));
        assertTrue(pluginYml.getString("commands.checkcommand.usage", "").contains("keyword"));
        assertTrue(pluginYml.isConfigurationSection("commands.sameip"));
        assertTrue(pluginYml.getString("commands.sameip.usage", "").contains("query"));
        assertTrue(pluginYml.isConfigurationSection("commands.lockip"));
        assertTrue(pluginYml.getString("commands.lockip.usage", "").contains("add"));
        assertTrue(pluginYml.getString("commands.lockip.permission", "").contains("lockip"));
        assertTrue(pluginYml.isConfigurationSection("commands.alert"));
        assertTrue(pluginYml.getString("commands.alert.usage", "").contains("add"));
        assertTrue(pluginYml.isConfigurationSection("commands.oplist"));
        assertTrue(pluginYml.getString("commands.oplist.permission", "").contains("oplist"));
        assertTrue(pluginYml.isConfigurationSection("commands.sudoall"));
        assertTrue(pluginYml.getString("commands.sudoall.usage", "").contains("chat"));
        assertTrue(pluginYml.getString("commands.sudoall.permission", "").contains("sudoall"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.admin.sudoall"));
        assertTrue(pluginYml.isConfigurationSection("commands.staffnote"));
        assertTrue(pluginYml.getString("commands.staffnote.usage", "").contains("remove"));
        assertTrue(pluginYml.isConfigurationSection("commands.note"));
        assertTrue(pluginYml.getString("commands.note.usage", "").contains("remove"));
        assertTrue(pluginYml.getString("commands.note.permission", "").contains("staffnote"));
        assertTrue(pluginYml.isConfigurationSection("commands.invcheck"));
        assertTrue(pluginYml.isConfigurationSection("commands.invload"));
        assertTrue(pluginYml.isConfigurationSection("commands.invlist"));
        assertTrue(pluginYml.isConfigurationSection("commands.invremove"));
        assertTrue(pluginYml.isConfigurationSection("commands.invremoveall"));
        assertTrue(pluginYml.isConfigurationSection("commands.clearinventory"));
        assertTrue(pluginYml.getStringList("commands.clearinventory.aliases").contains("ci"));
        assertTrue(pluginYml.getStringList("commands.clearinventory.aliases").contains("clear"));
        assertTrue(pluginYml.getString("commands.clearinventory.usage", "").contains("all"));
        assertTrue(pluginYml.isConfigurationSection("commands.enderchest"));
        assertTrue(pluginYml.getStringList("commands.enderchest.aliases").contains("ec"));
        assertTrue(pluginYml.getStringList("commands.enderchest.aliases").contains("ender"));
        assertTrue(pluginYml.getString("commands.enderchest.usage", "").contains("viewer"));
        assertTrue(pluginYml.isConfigurationSection("commands.clearender"));
        assertTrue(pluginYml.getStringList("commands.clearender.aliases").contains("clearenderchest"));
        assertTrue(pluginYml.getString("commands.clearender.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.condense"));
        assertTrue(pluginYml.getStringList("commands.condense.aliases").contains("compact"));
        assertTrue(pluginYml.isConfigurationSection("commands.uncondense"));
        assertTrue(pluginYml.getString("commands.uncondense.usage", "").contains("player"));
        assertTrue(pluginYml.getStringList("commands.uncondense.aliases").contains("uncompact"));
        assertTrue(pluginYml.getStringList("commands.trash.aliases").contains("disposal"));
        assertTrue(pluginYml.getStringList("commands.trash.aliases").contains("dispose"));
        assertTrue(pluginYml.getString("commands.trash.usage", "").contains("player"));
        assertTrue(pluginYml.getStringList("commands.workbench.aliases").contains("craft"));
        assertTrue(pluginYml.getStringList("commands.workbench.aliases").contains("wb"));
        assertTrue(pluginYml.getString("commands.workbench.usage", "").contains("player"));
        assertTrue(pluginYml.getStringList("commands.cartography.aliases").contains("cartographytable"));
        assertTrue(pluginYml.getStringList("commands.smithing.aliases").contains("smithingtable"));
        assertTrue(pluginYml.isConfigurationSection("commands.loom"));
        assertTrue(pluginYml.isConfigurationSection("commands.grindstone"));
        assertTrue(pluginYml.isConfigurationSection("commands.hat"));
        assertTrue(pluginYml.getString("commands.hat.usage", "").contains("player"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.hat.others"));
        assertTrue(pluginYml.isConfigurationSection("commands.skull"));
        assertTrue(pluginYml.getStringList("commands.skull.aliases").contains("head"));
        assertTrue(pluginYml.isConfigurationSection("commands.suicide"));
        assertTrue(pluginYml.getString("commands.suicide.usage", "").contains("player"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.suicide.others"));
        assertTrue(pluginYml.isConfigurationSection("commands.kill"));
        assertTrue(pluginYml.getString("commands.kill.usage", "").contains("-lightning"));
        assertTrue(pluginYml.getString("commands.kill.usage", "").contains("damageCause"));
        assertTrue(pluginYml.isConfigurationSection("commands.killall"));
        assertTrue(pluginYml.isString("commands.killall.usage"));
        assertTrue(pluginYml.isConfigurationSection("commands.groundclean"));
        assertTrue(pluginYml.getString("commands.groundclean.usage", "").contains("+cm"));
        assertTrue(pluginYml.isConfigurationSection("commands.remove"));
        assertTrue(pluginYml.getString("commands.remove.usage", "").contains("drops"));
        assertTrue(pluginYml.isConfigurationSection("commands.extinguish"));
        assertTrue(pluginYml.getStringList("commands.extinguish.aliases").contains("ext"));
        assertTrue(pluginYml.getString("commands.extinguish.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.burn"));
        assertTrue(pluginYml.getStringList("commands.burn.aliases").contains("ignite"));
        assertTrue(pluginYml.getString("commands.burn.usage", "").contains("time"));
        assertTrue(pluginYml.getString("commands.burn.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.lightning"));
        assertTrue(pluginYml.getStringList("commands.lightning.aliases").contains("smite"));
        assertTrue(pluginYml.getString("commands.lightning.usage", "").contains("world;x;y;z"));
        assertTrue(pluginYml.getString("commands.lightning.usage", "").contains("-safe"));
        assertTrue(pluginYml.getString("commands.lightning.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.exp"));
        assertTrue(pluginYml.getStringList("commands.exp.aliases").contains("xp"));
        assertTrue(pluginYml.isConfigurationSection("commands.checkexp"));
        assertTrue(pluginYml.isString("commands.checkexp.usage"));
        assertTrue(pluginYml.isConfigurationSection("commands.distance"));
        assertTrue(pluginYml.getStringList("commands.distance.aliases").contains("dist"));
        assertTrue(pluginYml.isConfigurationSection("commands.getpos"));
        assertTrue(pluginYml.getStringList("commands.getpos.aliases").contains("position"));
        assertTrue(pluginYml.isConfigurationSection("commands.compass"));
        assertTrue(pluginYml.getString("commands.compass.usage", "").contains("reset"));
        assertTrue(pluginYml.isConfigurationSection("commands.counter"));
        assertTrue(pluginYml.getString("commands.counter.usage", "").contains("msg:"));
        assertTrue(pluginYml.isConfigurationSection("commands.spawnmob"));
        assertTrue(pluginYml.getStringList("commands.spawnmob.aliases").contains("mob"));
        assertTrue(pluginYml.isConfigurationSection("commands.spawner"));
        assertTrue(pluginYml.getStringList("commands.spawner.aliases").contains("changems"));
        assertTrue(pluginYml.isConfigurationSection("commands.solve"));
        assertTrue(pluginYml.getString("commands.solve.usage", "").contains("equation"));
        assertTrue(pluginYml.isConfigurationSection("commands.sound"));
        assertTrue(pluginYml.getString("commands.sound.usage", "").contains("-p:"));
        assertTrue(pluginYml.isConfigurationSection("commands.shakeitoff"));
        assertTrue(pluginYml.getString("commands.shakeitoff.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.ride"));
        assertTrue(pluginYml.getString("commands.ride.permission", "").contains("ride"));
        assertTrue(pluginText.contains("hydroxide.admin.ride: true"));
        assertTrue(pluginText.contains("hydroxide.admin.ride.*: true"));
        assertTrue(pluginYml.isConfigurationSection("commands.findbiome"));
        assertTrue(pluginYml.getString("commands.findbiome.usage", "").contains("stopall"));
        assertTrue(pluginYml.getString("commands.findbiome.usage", "").contains("-r:"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.admin.findbiome"));
        assertTrue(pluginYml.isConfigurationSection("commands.fireball"));
        assertTrue(pluginYml.getString("commands.fireball.usage", "").contains("dragon"));
        assertTrue(pluginYml.getString("commands.fireball.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.kittycannon"));
        assertTrue(pluginYml.getStringList("commands.kittycannon.aliases").contains("ekittycannon"));
        assertTrue(pluginYml.getString("commands.kittycannon.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.beezooka"));
        assertTrue(pluginYml.getStringList("commands.beezooka.aliases").contains("beecannon"));
        assertTrue(pluginYml.getStringList("commands.beezooka.aliases").contains("ebeezooka"));
        assertTrue(pluginYml.getString("commands.beezooka.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.antioch"));
        assertTrue(pluginYml.getStringList("commands.antioch.aliases").contains("grenade"));
        assertTrue(pluginYml.getString("commands.antioch.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.nuke"));
        assertTrue(pluginYml.getStringList("commands.nuke.aliases").contains("enuke"));
        assertTrue(pluginYml.getString("commands.nuke.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.break"));
        assertTrue(pluginYml.getString("commands.break.permission", "").contains("break"));
        assertTrue(pluginYml.getString("commands.break.usage", "").contains("-s"));
        assertTrue(pluginYml.isConfigurationSection("commands.tree"));
        assertTrue(pluginYml.getString("commands.tree.usage", "").contains("-p:"));
        assertTrue(pluginYml.isConfigurationSection("commands.bigtree"));
        assertTrue(pluginYml.getString("commands.bigtree.permission", "").contains("tree"));
        assertTrue(pluginYml.getString("commands.bigtree.usage", "").contains("-p:"));
        assertTrue(pluginYml.isConfigurationSection("commands.launch"));
        assertTrue(pluginYml.getString("commands.launch.usage", "").contains("p:"));
        assertTrue(pluginYml.getString("commands.launch.usage", "").contains("-nodamage"));
        assertTrue(pluginYml.isConfigurationSection("commands.depth"));
        assertTrue(pluginYml.isConfigurationSection("commands.lastonline"));
        assertTrue(pluginYml.getString("commands.lastonline.usage", "").contains("-p:"));
        assertTrue(pluginYml.isConfigurationSection("commands.more"));
        assertTrue(pluginYml.getStringList("commands.more.aliases").contains("stack"));
        assertTrue(pluginYml.isConfigurationSection("commands.firework"));
        assertTrue(pluginYml.getStringList("commands.firework.aliases").contains("efirework"));
        assertTrue(pluginYml.getString("commands.firework.usage", "").contains("power"));
        assertTrue(pluginYml.getString("commands.firework.usage", "").contains("fire"));
        assertTrue(pluginYml.getString("commands.itemrepair.usage", "").contains("all"));
        assertTrue(pluginYml.getStringList("commands.itemrepair.aliases").contains("repair"));
        assertTrue(pluginYml.getStringList("commands.itemrepair.aliases").contains("fix"));
        assertTrue(pluginYml.isConfigurationSection("commands.dye"));
        assertTrue(pluginYml.getString("commands.dye.usage", "").contains("hex"));
        assertTrue(pluginYml.isConfigurationSection("commands.iteminfo"));
        assertTrue(pluginYml.getStringList("commands.iteminfo.aliases").contains("itemdb"));
        assertTrue(pluginYml.isConfigurationSection("commands.blockinfo"));
        assertTrue(pluginYml.isConfigurationSection("commands.entityinfo"));
        assertTrue(pluginYml.isConfigurationSection("commands.recipe"));
        assertTrue(pluginYml.getStringList("commands.recipe.aliases").contains("recipes"));
        assertTrue(pluginYml.isConfigurationSection("commands.powertool"));
        assertTrue(pluginYml.getStringList("commands.powertool.aliases").contains("pt"));
        assertTrue(pluginYml.isConfigurationSection("commands.powertooltoggle"));
        assertTrue(pluginYml.getStringList("commands.powertooltoggle.aliases").contains("ptt"));
        assertTrue(pluginYml.isConfigurationSection("commands.powertoollist"));
        assertTrue(pluginYml.getStringList("commands.powertoollist.aliases").contains("ptlist"));
        assertTrue(pluginYml.isConfigurationSection("commands.sell"));
        assertTrue(pluginYml.getStringList("commands.sell.aliases").contains("sellall"));
        assertTrue(pluginYml.isConfigurationSection("commands.worth"));
        assertTrue(pluginYml.getStringList("commands.worth.aliases").contains("price"));
        assertTrue(pluginYml.isConfigurationSection("commands.setworth"));
        assertTrue(pluginYml.getStringList("commands.setworth.aliases").contains("setprice"));
        assertTrue(pluginYml.isConfigurationSection("commands.generateworth"));
        assertTrue(pluginYml.getString("commands.generateworth.usage", "").contains("-overwrite"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.generateworth"));
        assertTrue(pluginYml.isConfigurationSection("commands.worthlist"));
        assertTrue(pluginYml.getString("commands.worthlist.usage", "").contains("-missing"));
        assertTrue(pluginText.contains("hydroxide.command.worthlist: true"));
        assertTrue(pluginText.contains("hydroxide.command.worthlist.others: true"));
        assertTrue(pluginYml.getStringList("commands.itemenchant.aliases").contains("enchant"));
        assertTrue(pluginYml.getString("commands.itemenchant.usage", "").contains("clear"));
        assertTrue(pluginYml.getStringList("commands.itemname.aliases").contains("rename"));
        assertTrue(pluginYml.getStringList("commands.itemlore.aliases").contains("lore"));
        assertTrue(pluginYml.isConfigurationSection("commands.hideflags"));
        assertTrue(pluginYml.getString("commands.hideflags.usage", "").contains("clear"));
        assertTrue(pluginYml.getStringList("commands.bookedit.aliases").contains("book"));
        assertTrue(pluginYml.getString("commands.bookedit.usage", "").contains("unlock"));
        assertTrue(pluginYml.isConfigurationSection("commands.anvilrepaircost"));
        assertTrue(pluginYml.getStringList("commands.anvilrepaircost.aliases").contains("repaircost"));
        assertTrue(pluginYml.isConfigurationSection("commands.unbreakable"));
        assertTrue(pluginYml.getString("commands.unbreakable.usage", "").contains("true"));
        assertTrue(pluginYml.isConfigurationSection("commands.itemframe"));
        assertTrue(pluginYml.getString("commands.itemframe.usage", "").contains("invisible"));
    }

    @Test
    void playtimeCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.playtime"));
        assertTrue(pluginYml.getString("commands.playtime.usage", "").contains("player"));
        assertTrue(pluginYml.isConfigurationSection("commands.cplaytime"));
        assertTrue(pluginYml.getString("commands.cplaytime.usage", "").contains("player"));
        assertTrue(pluginYml.getStringList("commands.cplaytime.aliases").contains("playtimegui"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.cplaytime.others"));
        assertTrue(pluginYml.isConfigurationSection("commands.editplaytime"));
        assertTrue(pluginYml.getString("commands.editplaytime.usage", "").contains("add"));
        assertTrue(pluginYml.getString("commands.editplaytime.usage", "").contains("-s"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.editplaytime"));
        assertTrue(pluginYml.isConfigurationSection("commands.playtimetop"));
        assertTrue(pluginYml.getStringList("commands.playtimetop.aliases").contains("ptop"));
    }

    @Test
    void proxyCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.server"));
        assertTrue(pluginYml.getString("commands.server.usage", "").contains("player"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.server.others"));
        assertTrue(pluginYml.isConfigurationSection("commands.serverlist"));
        assertTrue(pluginYml.getString("commands.serverlist.usage", "").contains("filter"));
        assertTrue(pluginYml.isConfigurationSection("commands.sendall"));
        assertTrue(pluginYml.getString("commands.sendall.permission", "").contains("sendall"));
        assertTrue(pluginYml.isConfigurationSection("commands.networkalert"));
    }

    @Test
    void kitParityCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.kit"));
        assertTrue(pluginYml.getString("commands.kit.usage", "").contains("player"));
        assertTrue(pluginYml.getString("commands.kit.usage", "").contains("-preview"));
        assertTrue(pluginYml.isConfigurationSection("commands.createkit"));
        assertTrue(pluginYml.getString("commands.createkit.permission", "").contains("createkit"));
        assertTrue(pluginYml.isConfigurationSection("commands.delkit"));
        assertTrue(pluginYml.getString("commands.delkit.permission", "").contains("delkit"));
        assertTrue(pluginYml.isConfigurationSection("commands.showkit"));
        assertTrue(pluginYml.getString("commands.showkit.permission", "").contains("showkit"));
        assertTrue(pluginYml.isConfigurationSection("commands.kitcdreset"));
        assertTrue(pluginYml.getString("commands.kitcdreset.usage", "").contains("all"));
    }

    @Test
    void worldMaintenanceCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.hydroworld"));
        assertTrue(pluginYml.isConfigurationSection("commands.gamerule"));
        assertTrue(pluginYml.getString("commands.gamerule.usage", "").contains("pvp"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.command.gamerule"));
        assertTrue(pluginYml.isConfigurationSection("commands.unloadchunks"));
        assertTrue(pluginYml.getString("commands.unloadchunks.usage", "").contains("-f"));
    }

    @Test
    void builderUtilityCommandsAreDeclaredForPaperRegistration() {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(pluginYml.isConfigurationSection("commands.blockcycling"));
        assertTrue(pluginYml.getStringList("commands.blockcycling.aliases").contains("blockcycle"));
        assertTrue(pluginYml.getString("commands.blockcycling.usage", "").contains("forward"));
        assertTrue(pluginYml.getString("commands.blockcycling.permission", "").contains("blockcycling"));
        assertTrue(pluginYml.isBoolean("permissions.hydroxide.admin.children.hydroxide.builder.blockcycling"));
    }
}
