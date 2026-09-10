package net.evolutiontiers.tagger.mixin;

import com.llamalad7.mixinextras.lib.semver.Version;
import net.evolutiontiers.tagger.TierTagger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
    @Unique
    private static final AtomicBoolean hasCheckedVersion = new AtomicBoolean(false);

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    public void showUpdateScreen(CallbackInfo ci) {
        if (hasCheckedVersion.get()) return;

        Version currentVersion = TierTagger.getCurrentVersion();
        Version latestVersion = TierTagger.getLatestVersion();

        if (TierTagger.isObsolete()) {
            Minecraft.getInstance().setScreen(new ConfirmScreen(
                    b -> {
                        if (b) {
                            Minecraft.getInstance().stop();
                        } else {
                            Minecraft.getInstance().setScreen(this);
                        }
                    },
                    Component.translatable("evolutiontiers.obsolete.title"),
                    Component.translatable("evolutiontiers.obsolete.desc"),
                    Component.translatable("menu.quit"),
                    Component.translatable("evolutiontiers.outdated.ignore")
            ));
        } else if (currentVersion != null && latestVersion != null && latestVersion.isHigherThan(currentVersion)) {
            Component newVersion = Component.literal(latestVersion.toString()).withStyle(ChatFormatting.GREEN);

            Minecraft.getInstance().setScreen(new ConfirmScreen(
                    b -> {
                        if (b) {
                            String url = "https://modrinth.com/mod/tiertagger/version/" + latestVersion;
                            Util.getPlatform().openUri(url);
                        }

                        Minecraft.getInstance().setScreen(this);
                    },
                    Component.translatable("evolutiontiers.outdated.title"),
                    Component.translatable("evolutiontiers.outdated.desc", newVersion),
                    Component.translatable("evolutiontiers.outdated.download"),
                    Component.translatable("evolutiontiers.outdated.ignore")
            ));
        }

        hasCheckedVersion.set(true);
    }
}
