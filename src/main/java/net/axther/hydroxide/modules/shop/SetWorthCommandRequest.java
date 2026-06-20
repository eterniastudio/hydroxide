package net.axther.hydroxide.modules.shop;

import java.util.Optional;

record SetWorthCommandRequest(WorthCommandRequest.Source source, Optional<String> material, double price) {
}
