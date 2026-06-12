package net.axther.hydroxide.modules.social;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PartyService {

    private final Map<UUID, Party> partiesByLeader = new HashMap<>();
    private final Map<UUID, UUID> leaderByMember = new HashMap<>();
    private final Map<UUID, UUID> invitedByTarget = new HashMap<>();

    public void invite(UUID leader, UUID target) {
        partiesByLeader.computeIfAbsent(leader, Party::new);
        leaderByMember.putIfAbsent(leader, leader);
        invitedByTarget.put(target, leader);
    }

    public boolean accept(UUID target) {
        UUID leader = invitedByTarget.remove(target);
        if (leader == null) {
            return false;
        }
        Party party = partiesByLeader.computeIfAbsent(leader, Party::new);
        party.members().add(target);
        leaderByMember.put(target, leader);
        leaderByMember.putIfAbsent(leader, leader);
        return true;
    }

    public void leave(UUID playerId) {
        UUID leader = leaderByMember.remove(playerId);
        if (leader == null) {
            return;
        }
        if (leader.equals(playerId)) {
            Party party = partiesByLeader.remove(leader);
            if (party != null) {
                party.members().forEach(leaderByMember::remove);
            }
        } else {
            partiesByLeader.getOrDefault(leader, new Party(leader)).members().remove(playerId);
        }
    }

    public boolean sameParty(UUID first, UUID second) {
        UUID firstLeader = leaderByMember.get(first);
        return firstLeader != null && firstLeader.equals(leaderByMember.get(second));
    }

    public boolean friendlyFireAllowed(UUID first, UUID second) {
        UUID leader = leaderByMember.get(first);
        if (leader == null || !leader.equals(leaderByMember.get(second))) {
            return true;
        }
        return partiesByLeader.get(leader).friendlyFire();
    }

    public void setFriendlyFire(UUID playerId, boolean allowed) {
        UUID leader = leaderByMember.get(playerId);
        if (leader != null) {
            partiesByLeader.get(leader).friendlyFire(allowed);
        }
    }

    public Optional<Party> party(UUID playerId) {
        UUID leader = leaderByMember.get(playerId);
        return leader == null ? Optional.empty() : Optional.ofNullable(partiesByLeader.get(leader));
    }

    public static final class Party {
        private final UUID leader;
        private final Set<UUID> members;
        private boolean friendlyFire;

        public Party(UUID leader) {
            this.leader = leader;
            this.members = new HashSet<>(Set.of(leader));
        }

        public UUID leader() {
            return leader;
        }

        public Set<UUID> members() {
            return members;
        }

        public boolean friendlyFire() {
            return friendlyFire;
        }

        void friendlyFire(boolean allowed) {
            this.friendlyFire = allowed;
        }
    }
}
