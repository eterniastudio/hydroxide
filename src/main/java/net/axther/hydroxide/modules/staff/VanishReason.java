package net.axther.hydroxide.modules.staff;

public enum VanishReason {
    PERSISTED_RESTORE("vanish persisted restore"),
    OP_AUTO_VANISH("op auto-vanish"),
    MANUAL_VANISH("manual vanish"),
    MANUAL_UNVANISH("manual unvanish"),
    FIX("fix"),
    RECONCILE("reconcile");

    private final String logText;

    VanishReason(String logText) {
        this.logText = logText;
    }

    public String logText() {
        return logText;
    }
}
