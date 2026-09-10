package net.evolutiontiers.tagger.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * The original (uku3lig) TierTagger let players switch between several
 * competing tier list providers (MCTiers, SubTiers) via a dropdown. This
 * fork only ever talks to one tier list, so this is now a single-entry
 * enum purely so the rest of the code (which used TierList.findByUrl, etc.)
 * doesn't need to change shape. Add more entries here later if this mod
 * ever needs to support switching providers again.
 */
@Getter
@AllArgsConstructor
public enum TierList {
    EVOLUTION_TIERS("Evolution Tiers", "https://api.evolutiontiers.dpdns.org", '\uE901'),
    ;

    private final String name;
    private final String url;
    private final char icon;

    public String styledName(boolean current) {
        String s = icon + " " + name;
        if (current) s += " (selected)";
        return s;
    }

    public static Optional<TierList> findByUrl(String url) {
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        final String finalUrl = url;
        return Arrays.stream(values()).filter(list -> list.url.equals(finalUrl)).findFirst();
    }
}
