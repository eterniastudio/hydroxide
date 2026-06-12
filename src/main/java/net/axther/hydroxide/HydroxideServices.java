package net.axther.hydroxide;

import net.axther.hydroxide.modules.economy.HydroEconomy;
import net.axther.hydroxide.modules.builder.BuilderService;
import net.axther.hydroxide.modules.nickname.NicknameService;
import net.axther.hydroxide.modules.options.PlayerOptionsService;
import net.axther.hydroxide.modules.staff.VanishService;
import net.axther.hydroxide.modules.stats.StatsService;
import net.axther.hydroxide.modules.welcome.PlayerVisualStateService;

import java.util.Optional;

public final class HydroxideServices {

    private NicknameService nicknameService;
    private HydroEconomy economy;
    private StatsService statsService;
    private VanishService vanishService;
    private PlayerVisualStateService playerVisualStateService;
    private BuilderService builderService;
    private PlayerOptionsService playerOptionsService;

    public Optional<NicknameService> nicknameService() {
        return Optional.ofNullable(nicknameService);
    }

    public void nicknameService(NicknameService nicknameService) {
        this.nicknameService = nicknameService;
    }

    public void clearNicknameService(NicknameService nicknameService) {
        if (this.nicknameService == nicknameService) {
            this.nicknameService = null;
        }
    }

    public Optional<HydroEconomy> economy() {
        return Optional.ofNullable(economy);
    }

    public void economy(HydroEconomy economy) {
        this.economy = economy;
    }

    public void clearEconomy(HydroEconomy economy) {
        if (this.economy == economy) {
            this.economy = null;
        }
    }

    public Optional<StatsService> statsService() {
        return Optional.ofNullable(statsService);
    }

    public void statsService(StatsService statsService) {
        this.statsService = statsService;
    }

    public void clearStatsService(StatsService statsService) {
        if (this.statsService == statsService) {
            this.statsService = null;
        }
    }

    public Optional<VanishService> vanishService() {
        return Optional.ofNullable(vanishService);
    }

    public void vanishService(VanishService vanishService) {
        this.vanishService = vanishService;
    }

    public void clearVanishService(VanishService vanishService) {
        if (this.vanishService == vanishService) {
            this.vanishService = null;
        }
    }

    public Optional<PlayerVisualStateService> playerVisualStateService() {
        return Optional.ofNullable(playerVisualStateService);
    }

    public void playerVisualStateService(PlayerVisualStateService playerVisualStateService) {
        this.playerVisualStateService = playerVisualStateService;
    }

    public void clearPlayerVisualStateService(PlayerVisualStateService playerVisualStateService) {
        if (this.playerVisualStateService == playerVisualStateService) {
            this.playerVisualStateService = null;
        }
    }

    public Optional<BuilderService> builderService() {
        return Optional.ofNullable(builderService);
    }

    public void builderService(BuilderService builderService) {
        this.builderService = builderService;
    }

    public void clearBuilderService(BuilderService builderService) {
        if (this.builderService == builderService) {
            this.builderService = null;
        }
    }

    public Optional<PlayerOptionsService> playerOptionsService() {
        return Optional.ofNullable(playerOptionsService);
    }

    public void playerOptionsService(PlayerOptionsService playerOptionsService) {
        this.playerOptionsService = playerOptionsService;
    }

    public void clearPlayerOptionsService(PlayerOptionsService playerOptionsService) {
        if (this.playerOptionsService == playerOptionsService) {
            this.playerOptionsService = null;
        }
    }
}
