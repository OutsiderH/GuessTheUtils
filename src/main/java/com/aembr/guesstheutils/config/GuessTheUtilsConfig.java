package com.aembr.guesstheutils.config;

import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.utils.Message;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.*;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class GuessTheUtilsConfig {
    public static final Path CONFIG_PATH = YACLPlatform.getConfigDir().resolve(
            "guesstheutils/");
    public static final String CONFIG_FILENAME = "config.json";

    public static final ConfigClassHandler<GuessTheUtilsConfig> CONFIG = ConfigClassHandler
            .createBuilder(GuessTheUtilsConfig.class)
            .id(Identifier.fromNamespaceAndPath(GuessTheUtils.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(CONFIG_PATH.resolve(CONFIG_FILENAME))
                    .build())
            .build();

    // CUSTOM SCOREBOARD

    public enum CustomScoreboardOption { ON, EXPANDED, OFF }
    public enum BuilderOffsetType { OFFSET, SHIFT }

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @TickBox
    public boolean enableCustomScoreboardModule = true;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @FloatSlider(min = 0.0f, max = 1.0f, step = 0.01f, format = "%.2f")
    public float customScoreboardBackgroundOpacity = 0.5f;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @FloatSlider(min = 0.0f, max = 1.0f, step = 0.01f, format = "%.2f")
    public float customScoreboardHighlightStrength = 0.1f;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @EnumCycler
    public CustomScoreboardOption customScoreboardShowPlaces = CustomScoreboardOption.EXPANDED;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @EnumCycler
    public CustomScoreboardOption customScoreboardShowTitles = CustomScoreboardOption.EXPANDED;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @EnumCycler
    public CustomScoreboardOption customScoreboardShowEmblems = CustomScoreboardOption.ON;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @EnumCycler
    public CustomScoreboardOption customScoreboardShowPoinsGainedInRound = CustomScoreboardOption.ON;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @TickBox
    public boolean customScoreboardTextShadow = true;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @IntSlider(min = 0, max = 5, step = 1)
    public int customScoreboardLinePadding = 1;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @IntSlider(min = 0, max = 5, step = 1)
    public int customScoreboardLineSpacing = 0;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @IntSlider(min = 0, max = 20, step = 1)
    public int customScoreboardSeparatorHeight = 6;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @TickBox
    public boolean customScoreboardDrawSeparatorBackground = true;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @IntSlider(min = 0, max = 20, step = 1)
    public int customScoreboardBuilderOffset = 0;

    @SerialEntry
    @AutoGen(category = "modules", group = "custom_scoreboard")
    @EnumCycler
    public BuilderOffsetType customScoreboardBuilderOffsetType = BuilderOffsetType.SHIFT;

    // SHORTCUT REMINDER

    public enum ShortcutFilterType { NONE, CJK, NON_ASCII }

    @SerialEntry
    @AutoGen(category = "modules", group = "shortcut_reminder")
    @TickBox
    public boolean enableShortcutReminderModule = true;

    @SerialEntry
    @AutoGen(category = "modules", group = "shortcut_reminder")
    @EnumCycler
    public ShortcutFilterType shortcutReminderFilterType = ShortcutFilterType.NON_ASCII;

    // CHAT COOLDOWN

    @SerialEntry
    @AutoGen(category = "modules", group = "chat_cooldown")
    @TickBox
    public boolean enableChatCooldownModule = true;

    @SerialEntry
    @AutoGen(category = "modules", group = "chat_cooldown")
    @IntSlider(min = 0, max = 100, step = 1)
    public int chatCooldownPingVolume = 50;

    @SerialEntry
    @AutoGen(category = "modules", group = "chat_cooldown")
    @TickBox
    public boolean chatCooldownTimer = true;

    // BUILDER NOTIFICATION

    @SerialEntry
    @AutoGen(category = "modules", group = "builder_notification")
    @TickBox
    public boolean enableBuilderNotificationModule = true;

    // DISALLOWED ITEM HIDER

    @SerialEntry
    @AutoGen(category = "modules", group = "disallowed_item_hider")
    @TickBox
    public boolean enableDisallowedItemHiderModule = true;

    // NAME AUTOCOMPLETE

    @SerialEntry
    @AutoGen(category = "modules", group = "name_autocomplete")
    @TickBox
    public boolean enableNameAutocompleteModule = true;

    public static Screen createScreen(@Nullable Screen parent) {
        if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            return CONFIG.generateGui().generateScreen(parent);
        } else {
            Message.displayMessage("YetAnotherConfigLib must be installed to use config menu!");
            return null;
        }
    }
}
