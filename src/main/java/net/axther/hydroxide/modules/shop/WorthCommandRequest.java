package net.axther.hydroxide.modules.shop;

import java.util.Optional;
import java.util.OptionalInt;

record WorthCommandRequest(Source source, Optional<String> material, OptionalInt amount) {

    enum Source {
        HAND,
        MATERIAL
    }
}
