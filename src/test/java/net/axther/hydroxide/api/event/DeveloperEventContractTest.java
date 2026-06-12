package net.axther.hydroxide.api.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeveloperEventContractTest {

    @Test
    void nicknameChangeEventIsCancellableAndCarriesRawNicknames() {
        PlayerNicknameChangeEvent event = new PlayerNicknameChangeEvent(null, null, "&7Old", "&#44CCFFNew");

        event.setCancelled(true);

        assertTrue(event.isCancelled());
        assertEquals("&7Old", event.oldNickname());
        assertEquals("&#44CCFFNew", event.newNickname());
    }

    @Test
    void economyTransactionEventRejectsUnsafeAmounts() {
        EconomyTransactionEvent event = new EconomyTransactionEvent(
                null,
                null,
                null,
                EconomyTransactionEvent.TransactionType.PAY,
                12.50,
                "test"
        );

        event.setAmount(10.25);

        assertEquals(10.25, event.amount());
        assertThrows(IllegalArgumentException.class, () -> event.setAmount(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> event.setAmount(1.999));
    }
}
