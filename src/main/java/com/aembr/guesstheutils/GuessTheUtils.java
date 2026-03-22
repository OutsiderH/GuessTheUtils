package com.aembr.guesstheutils;

import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import com.aembr.guesstheutils.modules.*;
import com.aembr.guesstheutils.utils.Message;
import com.aembr.guesstheutils.utils.TranslationData;
import com.aembr.guesstheutils.utils.Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
//? if <1.21.6
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;*/
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GuessTheUtils implements ClientModInitializer {
    public static final String MOD_ID = "guesstheutils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Minecraft CLIENT = Minecraft.getInstance();

    public static final MutableComponent prefix = Component.literal("[").withStyle(ChatFormatting.WHITE)
            .append(Component.literal("GTU").withStyle(ChatFormatting.GOLD))
            .append(Component.literal("] ").withStyle(ChatFormatting.WHITE));

    public static final Replay replay = new Replay();
    public static GTBEvents events = new GTBEvents();

    public static GameTracker gameTracker = new GameTracker(events);
    public static NameAutocomplete nameAutocomplete = new NameAutocomplete(events);
    public static ShortcutReminder shortcutReminder = new ShortcutReminder(events);
    @SuppressWarnings("unused")
    public static BuilderNotification builderNotification = new BuilderNotification(events);
    public static ChatCooldownTimer chatCooldown = new ChatCooldownTimer(events);

    public static boolean testing = false;
    public static LiveE2ERunner liveE2ERunner;

    private static Tick currentTick;
    private List<Component> previousScoreboardLines = new ArrayList<>();
    private List<Component> previousPlayerListEntries = new ArrayList<>();
    private Component previousActionBarMessage = Component.empty();
    private Component previousScreenTitle = Component.empty();

    public static boolean openConfig = false;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(
                (commandDispatcher,
                 commandRegistryAccess) -> Commands.register(commandDispatcher));

        ClientTickEvents.START_CLIENT_TICK.register(this::onStartTick);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) onActionBarMessage(message);
            else onChatMessage(message);
        });
        //? if <1.21.6 {
        /*//noinspection deprecation
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            chatCooldown.render(drawContext);
            gameTracker.scoreboard.render(drawContext);
        });
        *///?}

        replay.initialize();
        TranslationData.init();
        liveE2ERunner = new LiveE2ERunner(Replay.load(GuessTheUtils.class.getResourceAsStream("/assets/live_tests/TestBuggyLeaverDetection.json")));

        GuessTheUtilsConfig.CONFIG.load();
    }

    private void onStartTick(Minecraft client) {
        if (openConfig) {
            Screen configScreen = GuessTheUtilsConfig.createScreen(client.screen);
            client.setScreen(configScreen);
            openConfig = false;
        }

        if (client.player == null || events == null) return;
        if (currentTick == null) currentTick = new Tick();

        if (testing) {
            currentTick = liveE2ERunner.getNext();
        }

        if (!currentTick.isEmpty()) {
            replay.addTick(currentTick);
            try {
                Tick tempTick = currentTick;
                // Reset currentTick immediately. ConcurrentModificationException can be thrown if
                // a new message arrives while we're processing, modifying the chatMessage list
                currentTick = new Tick();

                events.processTickUpdate(tempTick);
            } catch (Exception e) {
                String stackTrace = Utils.getStackTraceAsString(e);

                events = null;
                Tick error = new Tick();
                error.error = stackTrace;
                replay.addTick(error);
                Message.displayMessage("Exception in GTBEvents: " + e.getMessage() + ". Saving details to replay file...");
                Message.displayMessage("Game restart required.");
                replay.save();
            } finally {
                currentTick = new Tick();
            }
        }

        if (testing) return;

        onScoreboardUpdate(Utils.getScoreboardLines(client));
        onPlayerListUpdate(Utils.collectTabListEntries(client));
        Component screenTitle = CLIENT.screen == null ? Component.empty() : CLIENT.screen.getTitle();
        onScreenUpdate(screenTitle);
    }

    private void onScreenUpdate(Component screenTitle) {
        if (currentTick == null) return;
        if (previousScreenTitle.equals(screenTitle)) return;
        previousScreenTitle = screenTitle;
        currentTick.screenTitle = screenTitle;
    }

    private void onScoreboardUpdate(List<Component> scoreboardLines) {
        if (currentTick == null) return;
        if (previousScoreboardLines.equals(scoreboardLines)) return;
        previousScoreboardLines = scoreboardLines;
        currentTick.scoreboardLines = scoreboardLines;
    }

    private void onPlayerListUpdate(List<Component> playerListEntries) {
        if (currentTick == null) return;
        if (previousPlayerListEntries.equals(playerListEntries)) return;
        previousPlayerListEntries = playerListEntries;
        currentTick.playerListEntries = playerListEntries;
    }

    private void onChatMessage(Component message) {
        // We don't want guild, party, or direct messages to be processed, or end up in replays
        String stripped = ChatFormatting.stripFormatting(message.getString());
        if (stripped == null
                || stripped.startsWith("Guild > ")
                || stripped.startsWith("Party > ")
                || stripped.startsWith("From ")) return;

        if (currentTick == null) return;
        if (currentTick.chatMessages == null) currentTick.chatMessages = new ArrayList<>();
        currentTick.chatMessages.add(message);
    }

    private void onActionBarMessage(Component message) {
        if (currentTick == null) return;
        if (previousActionBarMessage.equals(message)) return;
        previousActionBarMessage = message;
        currentTick.actionBarMessage = message;
    }

    public static void onTitleSet(Component title) {
        if (currentTick == null) return;
        currentTick.title = title;
    }

    public static void onSubtitleSet(Component subtitle) {
        if (currentTick == null) return;
        currentTick.subtitle = subtitle;
    }
}