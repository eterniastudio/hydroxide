package net.axther.hydroxide.modules.channels;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ChatChannelRouter {

    private final Map<String, ChatChannel> channels;

    public ChatChannelRouter(List<ChatChannel> channels) {
        this.channels = channels.stream().collect(Collectors.toUnmodifiableMap(
                channel -> channel.id().toLowerCase(Locale.ROOT),
                Function.identity()
        ));
    }

    public Optional<ChatChannel> channel(String id) {
        return Optional.ofNullable(channels.get(id.toLowerCase(Locale.ROOT)));
    }

    public boolean canReceive(String channelId, ChatParticipant sender, ChatParticipant viewer) {
        ChatChannel channel = channel(channelId).orElse(null);
        if (channel == null || !viewer.hasPermission(channel.permission())) {
            return false;
        }
        if (!channel.local()) {
            return true;
        }
        if (!sender.world().equals(viewer.world())) {
            return false;
        }
        double dx = sender.x() - viewer.x();
        double dy = sender.y() - viewer.y();
        double dz = sender.z() - viewer.z();
        return dx * dx + dy * dy + dz * dz <= channel.radius() * channel.radius();
    }
}
