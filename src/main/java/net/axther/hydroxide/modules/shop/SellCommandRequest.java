package net.axther.hydroxide.modules.shop;

import java.util.Optional;
import java.util.OptionalInt;

record SellCommandRequest(Mode mode, Optional<String> material, OptionalInt amount) {

    enum Mode {
        HAND,
        MATERIAL,
        ALL
    }
}
