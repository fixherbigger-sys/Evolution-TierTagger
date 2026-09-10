package net.evolutiontiers.tagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.llamalad7.mixinextras.lib.semver.Version;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.evolutiontiers.tagger.config.TierTaggerConfig;
import net.evolutiontiers.tagger.model.GameMode;
import net.evolutiontiers.tagger.model.PlayerInfo;
import net.uku3lig.ukulib.config.ConfigManager;
import net.uku3lig.ukulib.utils.Ukutils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class TierTagger {
    public static final String MOD_ID = "evolutiontiers";
    private static final String UPDATE_URL_FORMAT = "https://api.modrinth.com/v2/project/dpkYdLu5/version?game_versions=%s";

    public static final Gson GSON = new GsonBuilder().create();

    @Getter
    private static final ConfigManager<TierTaggerConfig> manager = ConfigManager.createDefault(TierTaggerConfig.class, MOD_ID);
    @Getter
    private static final Logger logger = LoggerFactory.getLogger(TierTagger.class);
    @Getter
    private static final HttpClient client = HttpClient.newHttpClient();

    // === version checker stuff ===
    @Getter
    private static Version latestVersion = null;
    @Getter
    private static Version currentVersion;
    private static final AtomicBoolean isObsolete = new AtomicBoolean(false);

    public static void onInitialize(Version currentVer) {
        currentVersion = currentVer;

        TierCache.init();

        Ukutils.registerKeybinding(new KeyMapping("evolutiontiers.keybind.gamemode", GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.register(Identifier.fromNamespaceAndPath("evolutiontiers", "key"))),
                mc -> {
                    GameMode next = TierCache.findNextMode(manager.getConfig().getGameMode());
                    manager.getConfig().setGameMode(next.id());

                    if (mc.player != null) {
                        Component message = Component.literal("Displayed gamemode: ").append(next.asStyled(false));
                        // Classic Gui#setOverlayMessage(Component, boolean) -- the newer
                        // Player#sendOverlayMessage doesn't exist on 1.21.11.
                        mc.gui.setOverlayMessage(message, false);
                    }
                });

        checkForUpdates();
    }

    public static Component appendTier(UUID uuid, Component text) {
        MutableComponent following = getPlayerTier(uuid)
                .map(entry -> {
                    Component tierText = getRankingText(entry.ranking(), false);

                    if (manager.getConfig().isShowIcons() && entry.mode() != null && entry.mode().icon().isPresent()) {
                        return Component.literal(entry.mode().icon().get().toString()).append(tierText);
                    } else {
                        return tierText.copy();
                    }
                })
                .orElse(null);

        if (following != null) {
            following.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));
            return following.append(text);
        }

        return text;
    }

    public static Optional<PlayerInfo.NamedRanking> getPlayerTier(UUID uuid) {
        GameMode mode = manager.getConfig().getGameMode();

        return TierCache.getPlayerRankings(uuid)
                .map(rankings -> {
                    PlayerInfo.Ranking ranking = rankings.get(mode.id());
                    Optional<PlayerInfo.NamedRanking> highest = PlayerInfo.getHighestRanking(rankings);
                    TierTaggerConfig.HighestMode highestMode = manager.getConfig().getHighestMode();

                    if (ranking == null) {
                        if (highestMode != TierTaggerConfig.HighestMode.NEVER && highest.isPresent()) {
                            return highest.get();
                        } else {
                            return null;
                        }
                    } else {
                        if (highestMode == TierTaggerConfig.HighestMode.ALWAYS && highest.isPresent()) {
                            return highest.get();
                        } else {
                            return ranking.asNamed(mode);
                        }
                    }
                });
    }

    private static MutableComponent getTierText(@org.jetbrains.annotations.Nullable String tier, boolean retired) {
        String text = retired ? "R" + (tier == null ? "??" : tier) : (tier == null ? "??" : tier);

        int color = TierTagger.getTierColor(text);
        return Component.literal(text).withStyle(s -> s.withColor(color));
    }

    public static Component getRankingText(PlayerInfo.Ranking ranking, boolean showPeak) {
        if (ranking.retired() && ranking.peakTier() != null) {
            return getTierText(ranking.peakTier(), true);
        } else {
            MutableComponent tierText = getTierText(ranking.tier(), false);

            if (showPeak && ranking.peakTier() != null && ranking.comparablePeak() < ranking.comparableTier()) {
                tierText.append(Component.literal(" (peak: ").withStyle(s -> s.withColor(ChatFormatting.GRAY)))
                        .append(getTierText(ranking.peakTier(), false))
                        .append(Component.literal(")").withStyle(s -> s.withColor(ChatFormatting.GRAY)));
            }

            return tierText;
        }
    }

    public static int getTierColor(String tier) {
        if (tier.startsWith("R")) {
            return manager.getConfig().getRetiredColor();
        } else {
            return manager.getConfig().getTierColors().getOrDefault(tier, 0xD3D3D3);
        }
    }

    /**
     * The original TierTagger checked Modrinth project "dpkYdLu5" (the
     * upstream uku3lig mod) for new versions. Since this fork isn't that
     * project, leaving that ID in place would compare THIS mod's version
     * against someone else's release history — it could wrongly flag this
     * mod as outdated/obsolete, or never update at all. Disabled until this
     * mod has its own Modrinth (or other) project to check against; swap
     * UPDATE_URL_FORMAT's project ID and re-enable the body below when it does.
     */
    private static void checkForUpdates() {
        logger.info("Update checking is disabled for this fork — see checkForUpdates() in TierTagger.java.");
    }

    public static boolean isObsolete() {
        return isObsolete.get();
    }
}