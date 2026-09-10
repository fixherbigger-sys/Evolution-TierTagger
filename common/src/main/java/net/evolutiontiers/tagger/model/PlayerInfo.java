package net.evolutiontiers.tagger.model;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import net.evolutiontiers.tagger.TierCache;
import net.evolutiontiers.tagger.TierTagger;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Mirrors the JSON shape returned by Evolution Tiers' /api/profile/uuid/{uuid}
 * and /api/profile/name/{ign} endpoints (see web/api.py's _build_profile).
 *
 * This replaces the original MCTiers-shaped PlayerInfo, which encoded tiers
 * as a (tier: int, pos: int) pair and had fields (badges, combat_master)
 * Evolution Tiers doesn't have. Tiers here are plain strings like "HT1" /
 * "LT3" / "Unranked", same as everywhere else in this project.
 */
public record PlayerInfo(String uuid, String ign, @SerializedName("avatar_url") @Nullable String avatarUrl,
                          String region, @SerializedName("total_points") int totalPoints,
                          @SerializedName("overall_rank") int overallRank,
                          @SerializedName("rank_title") String rankTitle,
                          Map<String, Ranking> rankings) {

    /**
     * Same ordering as tier_ranking in utils/constants.py (and web/api.py) —
     * lower index = better tier. Used to sort/compare tiers client-side
     * since the API gives us the tier as a plain string, not a numeric rank.
     */
    private static final List<String> TIER_ORDER = List.of(
            "HT1", "LT1", "HT2", "LT2", "HT3", "LT3", "HT4", "LT4", "HT5", "LT5", "Unranked"
    );

    public record Ranking(@Nullable String tier, @SerializedName("tier_full_name") @Nullable String tierFullName,
                           @SerializedName("tier_color") @Nullable String tierColor,
                           @SerializedName("tier_group") @Nullable String tierGroup,
                           @Nullable @SerializedName("peak_tier") String peakTier,
                           @Nullable @SerializedName("peak_tier_full_name") String peakTierFullName,
                           int points, @Nullable String region,
                           @SerializedName("is_retired") boolean retired,
                           @Nullable @SerializedName("attained_at") String attainedAt) {

        /**
         * Lower is better; unknown/null tiers sort last.
         */
        public int comparableTier() {
            int i = tier == null ? -1 : TIER_ORDER.indexOf(tier);
            return i < 0 ? Integer.MAX_VALUE : i;
        }

        /**
         * Lower is better; unknown/null peak tiers sort last.
         */
        public int comparablePeak() {
            int i = peakTier == null ? -1 : TIER_ORDER.indexOf(peakTier);
            return i < 0 ? Integer.MAX_VALUE : i;
        }

        public boolean isUnranked() {
            return tier == null || tier.equalsIgnoreCase("Unranked");
        }

        /**
         * The tier color the API sends is a "#RRGGBB" hex string; Minecraft's
         * TextColor/ARGB APIs want a bare int. Falls back to white if the
         * field is missing for some reason.
         */
        public int tierColorInt() {
            if (tierColor == null || tierColor.isEmpty()) return 0xFFFFFF;
            try {
                return Integer.parseInt(tierColor.replace("#", ""), 16);
            } catch (NumberFormatException e) {
                return 0xFFFFFF;
            }
        }

        /**
         * "attained_at" comes from the DB as a Postgres/SQLite timestamp
         * string (e.g. "2026-08-30 14:22:01"), not a unix epoch, since
         * that's how last_time_tested is stored server-side. Returns null
         * if it can't be parsed rather than throwing, since this is only
         * ever used for a "last tested" display line.
         */
        @Nullable
        public Instant attainedInstant() {
            if (attainedAt == null || attainedAt.isEmpty()) return null;
            try {
                return OffsetDateTime.parse(attainedAt.replace(" ", "T") + "Z").toInstant();
            } catch (Exception e) {
                return null;
            }
        }

        public NamedRanking asNamed(GameMode mode) {
            return new NamedRanking(mode, this);
        }
    }

    public record NamedRanking(@Nullable GameMode mode, Ranking ranking) {
    }

    private static final Map<String, Integer> REGION_COLORS = Map.of(
            "NA", 0xff6a6e,
            "EU", 0x6aff6e,
            "SA", 0xff9900,
            "AU", 0xf6b26b,
            "ME", 0xffd966,
            "AS/AU", 0xc27ba0,
            "AF", 0x674ea7
    );

    public static CompletableFuture<PlayerInfo> get(HttpClient client, UUID uuid) {
        String endpoint = TierTagger.getManager().getConfig().getApiUrl() + "/api/profile/uuid/" + uuid;
        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(s -> TierTagger.GSON.fromJson(s, PlayerInfo.class))
                .whenComplete((ignored, t) -> {
                    if (t != null) TierTagger.getLogger().warn("Error getting player info ({})", uuid, t);
                });
    }

    /**
     * The mod's per-player cache (see TierCache) only ever needs the
     * rankings map, not the whole profile, so this just unwraps get().
     * Evolution Tiers doesn't have a separate lightweight "rankings only"
     * endpoint like the original MCTiers API did, but a single profile
     * fetch is cheap enough that it doesn't matter.
     */
    public static CompletableFuture<Map<String, Ranking>> getRankings(HttpClient client, UUID uuid) {
        return get(client, uuid).thenApply(info -> info == null ? Map.of() : info.rankings());
    }

    public static CompletableFuture<PlayerInfo> search(HttpClient client, String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String endpoint = TierTagger.getManager().getConfig().getApiUrl() + "/api/profile/name/" + encoded;
        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(s -> TierTagger.GSON.fromJson(s, PlayerInfo.class))
                .whenComplete((ignored, t) -> {
                    if (t != null) TierTagger.getLogger().warn("Error searching player {}", query, t);
                });
    }

    public int getRegionColor() {
        if (this.region == null) return 0xffffff;
        return REGION_COLORS.getOrDefault(this.region.toUpperCase(Locale.ROOT), 0xffffff);
    }

    public static Optional<NamedRanking> getHighestRanking(Map<String, Ranking> rankings) {
        return rankings.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .min(Comparator.comparingInt(e -> e.getValue().comparableTier()))
                .map(e -> e.getValue().asNamed(TierCache.findModeOrUgly(e.getKey())));
    }

    /**
     * Evolution Tiers computes the display title (rankTitle, e.g. "Combat
     * Master") server-side from the same thresholds every other part of
     * the site uses (see RANK_TITLES in web/api.py), so this just maps
     * that string to a display color rather than re-deriving thresholds
     * client-side and risking the two falling out of sync.
     */
    public int getPointInfoColor() {
        return switch (this.rankTitle == null ? "" : this.rankTitle) {
            case "Combat Grandmaster" -> 0xE6C622;
            case "Combat Master" -> 0xFBB03B;
            case "Combat Ace" -> 0xCD285C;
            case "Combat Specialist" -> 0xAD78D8;
            case "Combat Cadet" -> 0x9291D9;
            case "Combat Novice" -> 0x9291D9;
            case "Rookie" -> 0x6C7178;
            default -> 0xFFFFFF; // "Unranked" or anything unrecognised
        };
    }

    public int getPointInfoAccentColor() {
        return switch (this.rankTitle == null ? "" : this.rankTitle) {
            case "Combat Grandmaster" -> 0xFDE047;
            case "Combat Master" -> 0xFFD13A;
            case "Combat Ace" -> 0xD65474;
            case "Combat Specialist" -> 0xC7A3E8;
            case "Combat Cadet" -> 0xADACE2;
            case "Combat Novice" -> 0xFFFFFF;
            case "Rookie" -> 0x8B979C;
            default -> 0xFFFFFF;
        };
    }

    public List<NamedRanking> getSortedTiers() {
        List<NamedRanking> tiers = new ArrayList<>(this.rankings.entrySet().stream()
                .map(e -> e.getValue().asNamed(TierCache.findModeOrUgly(e.getKey())))
                .toList());

        tiers.sort(Comparator.comparing((NamedRanking a) -> a.ranking.retired, Boolean::compare)
                .thenComparingInt(a -> a.ranking.comparableTier()));

        return tiers;
    }
}
