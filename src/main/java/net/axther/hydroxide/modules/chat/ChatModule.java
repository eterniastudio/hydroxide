package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import java.util.List;

public final class ChatModule implements HydroModule {

    private ChatListener listener;

    @Override
    public String id() {
        return "chat";
    }

    @Override
    public String displayName() {
        return "Chat";
    }

    @Override
    public String description() {
        return "Modern Adventure chat formatting, broadcast, private messages, and replies.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        listener = new ChatListener(context);
        MessageCommands messageCommands = new MessageCommands(context);
        Bukkit.getPluginManager().registerEvents(listener, context.plugin());
        context.commands().register("broadcast", new BroadcastCommand(context));
        context.commands().register("message", messageCommands);
        context.commands().register("reply", messageCommands);
    }

    @Override
    public void onDisable(HydroxideContext context) {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
    }
}
