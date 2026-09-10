package net.evolutiontiers.tagger.config;

import net.evolutiontiers.tagger.TierCache;
import net.evolutiontiers.tagger.TierTagger;
import net.evolutiontiers.tagger.tierlist.PlayerSearchScreen;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.uku3lig.ukulib.config.option.*;
import net.uku3lig.ukulib.config.option.widget.ButtonTab;
import net.uku3lig.ukulib.config.screen.TabbedConfigScreen;

import java.util.*;
import java.util.stream.Collectors;

public class TTConfigScreen extends TabbedConfigScreen<TierTaggerConfig> {
    public TTConfigScreen(Screen parent) {
        super("Evolution Tiers Config", parent, TierTagger.getManager());
    }

    @Override
    protected Tab[] getTabs(TierTaggerConfig config) {
        // Only one tier list provider now (see model/TierList.java), so the
        // old provider-switching tab has been removed entirely.
        return new Tab[]{new MainSettingsTab(), new ColorsTab()};
    }

    public class MainSettingsTab extends ButtonTab<TierTaggerConfig> {
        public MainSettingsTab() {
            super("evolutiontiers.config", TTConfigScreen.this.manager);
        }

        @Override
        protected WidgetCreator[] getWidgets(TierTaggerConfig config) {
            return new WidgetCreator[]{
                    CyclingOption.ofBoolean("evolutiontiers.config.enabled", config.isEnabled(), config::setEnabled),
                    new CyclingOption<>("evolutiontiers.config.gamemode", TierCache.getGamemodes(), config.getGameMode(), m -> config.setGameMode(m.id()), m -> Component.literal(m.title()),
                            m -> m.isNone() ? Tooltip.create(Component.translatable("evolutiontiers.config.gamemode.none")) : null, !config.getGameMode().isNone()),
                    CyclingOption.ofBoolean("evolutiontiers.config.retired", config.isShowRetired(), config::setShowRetired),
                    CyclingOption.ofTranslatableEnum("evolutiontiers.config.highest", TierTaggerConfig.HighestMode.class, config.getHighestMode(), config::setHighestMode, OptionInstance.cachedConstantTooltip(Component.translatable("evolutiontiers.config.highest.desc"))),
                    CyclingOption.ofBoolean("evolutiontiers.config.icons", config.isShowIcons(), config::setShowIcons),
                    CyclingOption.ofBoolean("evolutiontiers.config.playerList", config.isPlayerList(), config::setPlayerList),
                    new SimpleButton("evolutiontiers.clear", ignored -> TierCache.clearCache()),
                    new ScreenOpenButton("evolutiontiers.config.search", PlayerSearchScreen::new)
            };
        }
    }

    public class ColorsTab extends ButtonTab<TierTaggerConfig> {
        protected ColorsTab() {
            super("evolutiontiers.colors", TTConfigScreen.this.manager);
        }

        @Override
        protected WidgetCreator[] getWidgets(TierTaggerConfig config) {
            // i genuinely don't understand but chaining the calls just EXPLODES????
            Comparator<Map.Entry<String, Integer>> comparator = Comparator.comparing(e -> e.getKey().charAt(2));
            comparator = comparator.thenComparing(e -> e.getKey().charAt(0));

            List<ColorOption> tiers = config.getTierColors().entrySet().stream()
                    .sorted(comparator)
                    .map(e -> new ColorOption(e.getKey(), e.getValue(), val -> config.getTierColors().put(e.getKey(), val)))
                    .collect(Collectors.toList());

            tiers.addLast(new ColorOption("evolutiontiers.colors.retired", config.getRetiredColor(), config::setRetiredColor));

            return tiers.toArray(WidgetCreator[]::new);
        }
    }
}
