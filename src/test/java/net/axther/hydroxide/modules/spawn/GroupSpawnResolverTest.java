package net.axther.hydroxide.modules.spawn;

import net.axther.hydroxide.storage.StoredLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupSpawnResolverTest {

    @Test
    void resolvesHighestPriorityMatchingGroupBeforeDefault() {
        StoredLocation guest = new StoredLocation("world", 1, 64, 1, 0, 0);
        StoredLocation vip = new StoredLocation("world", 10, 70, 10, 90, 0);
        StoredLocation fallback = new StoredLocation("world", 0, 65, 0, 0, 0);
        GroupSpawnResolver resolver = new GroupSpawnResolver(List.of(
                new GroupSpawnResolver.Entry("guest", 1, guest),
                new GroupSpawnResolver.Entry("vip", 10, vip),
                new GroupSpawnResolver.Entry("default", 0, fallback)
        ));

        Optional<StoredLocation> resolved = resolver.resolve(List.of("guest", "vip"));

        assertEquals(Optional.of(vip), resolved);
    }
}
