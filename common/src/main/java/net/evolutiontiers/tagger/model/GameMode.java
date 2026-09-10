package net.evolutiontiers.tagger.model;

import com.google.gson.JsonArray;
import net.evolutiontiers.tagger.TierTagger;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record GameMode(String id, String title) {
    public static final GameMode NONE = new GameMode("annoying_long_id_that_no_one_will_ever_use_just_to_make_sure", "§cNone§r");

    /**
     * Evolution Tiers' /api/gamemodes returns a JSON ARRAY of
     * {"key": ..., "label": ..., "active": ...} objects (see web/api.py's
     * _get_live_gamemodes), unlike the original MCTiers /v2/mode/list which
     * returned an object keyed by mode id. Only "active" gamemodes (i.e.
     * ones whose Discord bot is actually online right now) are kept, since
     * inactive ones have no ranking data to show anyway.
     */
    public static CompletableFuture<List<GameMode>> fetchGamemodes(HttpClient client) {
        String endpoint = TierTagger.getManager().getConfig().getApiUrl() + "/api/gamemodes";
        final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())

                .thenApply(r -> {
                    JsonArray array = TierTagger.GSON.fromJson(r.body(), JsonArray.class);

                    return array.asList().stream()
                            .map(com.google.gson.JsonElement::getAsJsonObject)
                            .filter(obj -> !obj.has("active") || obj.get("active").getAsBoolean())
                            .map(obj -> new GameMode(obj.get("key").getAsString(), obj.get("label").getAsString()))
                            .toList();
                });
    }

    public boolean isNone() {
        return this.id.equals(NONE.id);
    }

    private Pair<Character, TextColor> iconAndColor() {
        return switch (this.id) {
            case "axe" -> Pair.of('\uE701', TextColor.fromLegacyFormat(ChatFormatting.GREEN));
            case "mace" -> Pair.of('\uE702', TextColor.fromLegacyFormat(ChatFormatting.GRAY));
            case "nethpot" -> Pair.of('\uE703', TextColor.fromRgb(0x7d4a40));
            case "pot" -> Pair.of('\uE704', TextColor.fromRgb(0xff0000));
            case "smp" -> Pair.of('\uE705', TextColor.fromRgb(0xeccb45));
            case "sword" -> Pair.of('\uE706', TextColor.fromRgb(0xa4fdf0));
            case "uhc" -> Pair.of('\uE707', TextColor.fromLegacyFormat(ChatFormatting.RED));
            case "vanilla" -> Pair.of('\uE708', TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE));
            // Evolution Tiers only ever serves the 8 gamemodes above (see
            // web/api.py's GAMEMODES list); the original mod's SubTiers-only
            // gamemode icons (bed, bow, elytra, etc.) have been removed
            // along with their now-unused texture assets.
            default -> Pair.of('•', TextColor.fromLegacyFormat(ChatFormatting.WHITE));
        };
    }

    public Optional<Character> icon() {
        Pair<Character, TextColor> pair = this.iconAndColor();

        return pair.right().getValue() == 0xFFFFFF ? Optional.empty() : Optional.of(pair.left());
    }

    public Component asStyled(boolean withDefaultDot) {
        Pair<Character, TextColor> pair = this.iconAndColor();

        if (pair.right().getValue() == 0xFFFFFF && !withDefaultDot) {
            return Component.literal(this.title);
        } else {
            Component name = Component.literal(this.title).withStyle(s -> s.withColor(pair.right()));
            return Component.literal(pair.left() + " ").append(name);
        }
    }
}
