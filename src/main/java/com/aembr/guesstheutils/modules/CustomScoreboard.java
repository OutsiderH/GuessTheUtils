package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
//? if >=1.21.6 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
//? if >=1.21.6
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
//? if >=1.21.6
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.aembr.guesstheutils.GuessTheUtils.CLIENT;
import static com.aembr.guesstheutils.GuessTheUtils.events;

public class CustomScoreboard /*? >=1.21.6 {*/ implements HudElement /*?}*/ {
    public static final String[] BUILDING_SPINNER = new String[] {"\uea00", "\uea01", "\uea02", "\uea03", "\uea04",
            "\uea05", "\uea06", "\uea07", "\uea08", "\uea09", "\uea10", "\uea11", "\uea12", "\uea13", "\uea14", "\uea15"};
    public static final String[] POINTS_ICONS = new String[] {"+1", "+2", "+3"};
    public static final String INACTIVE_ICON = "\uea19";
    public static final String LEAVER_ICON = "\uea20";
    public static final String BUILD_BG_ICON = "\uea21";
    public static final String BUILD_CHECK_ICON = "\uea22";

    public static final int INACTIVE_PLAYER_THRESHOLD_SECONDS = 180;
    public static int tickCounter = 0;

    public static ChatFormatting backgroundColor = ChatFormatting.BLACK;
    public static ChatFormatting textColor = ChatFormatting.WHITE;
    public static float foregroundOpacity = 1.0f;
    public static float foregroundOpacityInactive = 0.4f;

    public static ChatFormatting accentColor = ChatFormatting.GREEN;
    public static ChatFormatting accentColorBuilder = ChatFormatting.AQUA;

    public static ChatFormatting notBuiltIconColor = ChatFormatting.DARK_GRAY;
    public static float notBuiltIconOpacity = 0.5f;
    public static ChatFormatting inactiveIconColor = ChatFormatting.DARK_GRAY;
    public static ChatFormatting leaverIconColor = ChatFormatting.RED;

    public static ChatFormatting pointsThisRoundColor1 = ChatFormatting.DARK_GREEN;
    public static ChatFormatting pointsThisRoundColor2 = ChatFormatting.GREEN;
    public static ChatFormatting pointsThisRoundColor3 = ChatFormatting.YELLOW;
    public static float pointsThisRoundOpacity = 0.5f;

    public static ChatFormatting pointsColor = ChatFormatting.DARK_GREEN;
    public static ChatFormatting pointsColorHighlight = ChatFormatting.GREEN;

    public static ChatFormatting unknownThemeColor = ChatFormatting.RED;
    public static String unknownThemeString = "???";

    public static int lineItemSpacing = 4;
    public static int heightOffset = 20;
    public static int playerNameRightPad = 4;

    static GameTracker tracker;
    //? if >=1.21.6
    Identifier identifier = Identifier.parse("guess_the_utils_scoreboard");

    public CustomScoreboard(GameTracker tracker) {
        CustomScoreboard.tracker = tracker;
        //? if >=1.21.6 {
        try {
            HudElementRegistry.attachElementAfter(Identifier.withDefaultNamespace("chat"), identifier, this);
        } catch (Exception e) {
            HudElementRegistry.replaceElement(identifier, hudElement -> this);
        }
        //?}
    }

    public static boolean isRendering() {
        return tracker != null && tracker.game != null && events != null
                && GuessTheUtils.events.isInGtb()
                && GuessTheUtilsConfig.CONFIG.instance().enableCustomScoreboardModule;
    }

    public static String getLengthAsString(String input) {
        String[] words = input.split(" ");
        StringBuilder lengths = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            lengths.append(words[i].length());
            if (i < words.length - 1) {
                lengths.append("-");
            }
        }
        return lengths.toString();
    }

    public static String getSpinnerFrame(String[] frames, int ticksPerFrame, int tickCounter) {
        int index = (tickCounter / ticksPerFrame) % frames.length;
        return frames[frames.length - 1 - index]; // uhh it's reversed lol
    }

    private static void drawTextRightAligned(GuiGraphics context, Font renderer, String string, int x, int y,
                                             int color, boolean shadow) {
        context.drawString(renderer, string, x - renderer.width(string), y, color, shadow);
    }

    private static void drawTextRightAligned(GuiGraphics context, Font renderer, Component text, int x, int y,
                                             int color, boolean shadow) {
        context.drawString(renderer, text, x - renderer.width(text), y, color, shadow);
    }

    @SuppressWarnings({"unused", "SameParameterValue", "DuplicateExpressions"})
    private static int drawLine(GuiGraphics context, Font renderer, ScoreboardLine line, int x, int y,
                                int width, int linePadding, boolean includeTitles, boolean includeEmblems,
                                boolean includeRoundPoints, int lineItemSpacing, int lineSpacing, int backgroundColor,
                                int textColor, ChatFormatting textColorFormatting, int textColorInactive, int textColorPointsThisRound,
                                ChatFormatting accentColor, ChatFormatting accentColorBuilder, int backgroundHighlightColor,
                                int backgroundHighlightColorBuilder, ChatFormatting notBuiltIconColor,
                                float notBuildIconOpacity, ChatFormatting inactiveIconColor, ChatFormatting leaverIconColor,
                                ChatFormatting pointsThisRoundColor1, ChatFormatting pointsThisRoundColor2,
                                ChatFormatting pointsThisRoundColor3, ChatFormatting pointsColor,
                                ChatFormatting pointsColorHighlight, GameTracker.Game game, int playerPlace, boolean shadow,
                                boolean drawSeparatorBg, boolean includePlaces) {

        if (line instanceof SeparatorLine) {
            if (drawSeparatorBg) {
                context.fill(x, y, x + width, y + ((SeparatorLine) line).height - 2 + linePadding * 2, backgroundColor);
            }
            return ((SeparatorLine) line).height;
        }

        int itemX = x + linePadding;
        int itemY = y + linePadding;

        if (line instanceof PlayerLine) {
            int builderOffset = GuessTheUtilsConfig.CONFIG.instance().customScoreboardBuilderOffset;
            GuessTheUtilsConfig.BuilderOffsetType builderOffsetType =
                    GuessTheUtilsConfig.CONFIG.instance().customScoreboardBuilderOffsetType;

            GameTracker.Player player = ((PlayerLine) line).player;

            boolean isBuilder = Objects.equals(game.currentBuilder, player);
            boolean isBuildingThisRound = player.buildRound == game.currentRound;
            boolean isRoundPre = GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE);
            int pointsThisRound = isRoundPre ? 0 : player.points[game.currentRound - 1];

            int fgColor = (!player.leaverState.equals(GameTracker.Player.LeaverState.NORMAL)
                    || player.inactiveTicks > INACTIVE_PLAYER_THRESHOLD_SECONDS * 20) ? textColorInactive : textColor;
            int bottom = y + renderer.lineHeight - 2 + linePadding * 2;

            if (isBuilder) {
                x -= builderOffset;
                itemX = x + linePadding;
                if (builderOffsetType.equals(GuessTheUtilsConfig.BuilderOffsetType.OFFSET)) {
                    width += builderOffset;
                }
            }

            context.fill(x, y, x + width, y + renderer.lineHeight - 2 + linePadding * 2, backgroundColor);

            // Highlight
            int highlightColor = isBuilder ? backgroundHighlightColorBuilder : backgroundHighlightColor;
            if (pointsThisRound > 0 || isBuilder) {
                context.fill(x, y, x + width, bottom, highlightColor);
            }

            // Build Icon BG
            Component builderIconBg = Component.literal(BUILD_BG_ICON).withStyle(notBuiltIconColor);
            drawTextRightAligned(context, renderer, builderIconBg, itemX - linePadding - lineItemSpacing, itemY, rgbToArgb(textColor, notBuildIconOpacity), shadow);

            // Build Icon Check or Spinner
            if (player.buildRound != 0) {
                Component builderIconFg = Component.literal(BUILD_CHECK_ICON).withStyle(accentColorBuilder);
                if (isBuildingThisRound && GameTracker.state.equals(GTBEvents.GameState.ROUND_BUILD)) {
                    builderIconFg = Component.literal(getSpinnerFrame(BUILDING_SPINNER, 1, tickCounter))
                            .withStyle(accentColorBuilder);
                }
                context.drawString(renderer, builderIconFg, itemX - linePadding - lineItemSpacing - renderer.width(builderIconBg), itemY, textColor, shadow);
            }

            // Places
            if (includePlaces) {
                int placeWidth = game.players.size() == 10 ? renderer.width("00") : renderer.width("0");
                Component place = Component.literal(String.valueOf(playerPlace)).withStyle(textColorFormatting);
                drawTextRightAligned(context, renderer, place, itemX + placeWidth, itemY, fgColor, shadow);
                itemX += placeWidth + lineItemSpacing;
            }

            // Leaver Badge
            Component badge = Component.empty();
            if (!player.leaverState.equals(GameTracker.Player.LeaverState.NORMAL)) {
                badge = Component.literal(LEAVER_ICON).withStyle(leaverIconColor);
            } else if (player.inactiveTicks > INACTIVE_PLAYER_THRESHOLD_SECONDS * 20) {
                badge = Component.literal(INACTIVE_ICON).withStyle(inactiveIconColor);
            }
            if (!badge.getString().isEmpty()) {
                context.drawString(renderer, badge, itemX, itemY, textColor, shadow);
                itemX += renderer.width(badge) + lineItemSpacing;
            }

            // Name
            ChatFormatting rankColor = player.rank == null ? ChatFormatting.GRAY : player.rank;
            MutableComponent name = Component.literal(player.name).withStyle(rankColor);

            if (includeEmblems && player.emblem != null && !player.emblem.getString().isEmpty()) {
                name.append(Component.literal(" ")).append(player.emblem);
            }

            if (includeTitles && player.title != null) {
                MutableComponent title = player.title.copy();
                name = title.append(Component.literal(" ")).append(name);
            }

            context.drawString(renderer, name, itemX, itemY, fgColor, shadow);

            // Points
            itemX = x + width - linePadding;
            MutableComponent points = Component.literal(String.valueOf(player.getTotalPoints()));
            if (!isRoundPre && pointsThisRound > 0) {
                if (isBuildingThisRound) points.withStyle(accentColorBuilder);
                else points.withStyle(pointsColorHighlight);
            } else points.withStyle(pointsColor);

            int pointsWidth = (player.getTotalPoints() < 10 ? renderer.width("0") : renderer.width("00")) + 1;

            drawTextRightAligned(context, renderer, points, itemX, itemY, fgColor, shadow);

            // Points this round
            if (!isRoundPre && pointsThisRound > 0 && includeRoundPoints) {
                itemX -= pointsWidth + lineItemSpacing;
                MutableComponent pointsThisRoundIcon = Component.literal(POINTS_ICONS[pointsThisRound - 1]);
                ChatFormatting pointsThisRoundColor;
                if (isBuildingThisRound) pointsThisRoundIcon.withStyle(accentColorBuilder);
                else {
                    switch (pointsThisRound) {
                        case 3: pointsThisRoundIcon.withStyle(pointsThisRoundColor3);
                        case 2: pointsThisRoundIcon.withStyle(pointsThisRoundColor2);
                        case 1: pointsThisRoundIcon.withStyle(pointsThisRoundColor1);
                    }
                }
                drawTextRightAligned(context, renderer, pointsThisRoundIcon, itemX, itemY, textColorPointsThisRound, shadow);
            }

        }

        if (line instanceof TextLine) {
            context.fill(x, y, x + width, y + renderer.lineHeight - 2 + linePadding * 2, backgroundColor);

            for (Component item : ((TextLine) line).left()) {
                context.drawString(renderer, item, itemX, itemY, textColor, shadow);
                itemX += renderer.width(item) + lineItemSpacing;
            }

            AtomicInteger centerItemsWidth = new AtomicInteger();
            ((TextLine) line).center().forEach(i -> centerItemsWidth.addAndGet(renderer.width(i)));
            centerItemsWidth.addAndGet((((TextLine) line).center().size() - 1) * lineItemSpacing);

            itemX = x + width / 2 - centerItemsWidth.get() / 2;

            for (Component item : ((TextLine) line).center()) {
                context.drawString(renderer, item, itemX, itemY, textColor, shadow);
                itemX += renderer.width(item) + lineItemSpacing;
            }

            itemX = x + width - linePadding;

            for (Component item : ((TextLine) line).right()) {
                drawTextRightAligned(context, renderer, item, itemX, itemY, textColor, shadow);
                itemX -= renderer.width(item) + lineItemSpacing;
            }
        }
        return renderer.lineHeight - 2 + linePadding * 2 + lineSpacing;
    }

    private static int getTotalWidth(Font renderer, List<ScoreboardLine> lines, int linePadding,
                                     boolean includeTitles, boolean includeEmblems, boolean includePointsGainedInRound,
                                     int lineItemSpacing, int playerNameRightPad, boolean includePlaces, GameTracker.Game game) {
        int width = 0;
        for (ScoreboardLine line : lines) {
            if (line instanceof SeparatorLine) continue;
            if (line instanceof PlayerLine) {
                GameTracker.Player player = ((PlayerLine) line).player;

                int placeWidth = 0;
                if (includePlaces) {
                    placeWidth = (game.players.size() == 10 ? renderer.width("00")
                            : renderer.width("0")) + lineItemSpacing;
                }

                int leaverBadgeWidth = player.leaverState.equals(GameTracker.Player.LeaverState.NORMAL) ? 0
                        : renderer.width(LEAVER_ICON) + lineItemSpacing;

                int nameWidth = renderer.width(player.name) + playerNameRightPad + lineItemSpacing;

                if (includeEmblems && player.emblem != null && !player.emblem.getString().isEmpty()) {
                    nameWidth += renderer.width(Component.literal(" ").append(player.emblem));
                }

                if (includeTitles && player.title != null) {
                    nameWidth += renderer.width(Component.literal(" ").append(player.title));
                }

                int pointsThisRoundWidth;
                if (includePointsGainedInRound) {
                    pointsThisRoundWidth = renderer.width("+3") + lineItemSpacing;
                } else {
                    pointsThisRoundWidth = 0;
                }

                int pointsWidth = player.getTotalPoints() >= 10 ? renderer.width("00") : renderer.width("0");

                int totalWidth = linePadding * 2 + placeWidth + leaverBadgeWidth + nameWidth + pointsThisRoundWidth + pointsWidth;

                if (totalWidth > width) width = totalWidth;
            }
            if (line instanceof TextLine) {
                AtomicInteger lineWidth = new AtomicInteger();
                AtomicInteger items = new AtomicInteger();
                Stream.of(((TextLine) line).left, ((TextLine) line).center, ((TextLine) line).right)
                        .flatMap(List::stream).forEach(i -> {
                            lineWidth.addAndGet(renderer.width(i));
                            items.addAndGet(1);
                        });
                lineWidth.addAndGet(Math.max(0, items.get() - 1) * lineItemSpacing + linePadding * 2);
                if (lineWidth.get() > width) width = lineWidth.get();
            }
        }
        return width;
    }

    public static int rgbToArgb(int rgb, float alpha) {
        if (alpha < 0.0f) alpha = 0.0f;
        if (alpha > 1.0f) alpha = 1.0f;

        int alphaInt = (int) (alpha * 255);

        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        return (alphaInt << 24) | (red << 16) | (green << 8) | blue;
    }

    @SuppressWarnings({"DataFlowIssue"})
    //? if >=1.21.6
    @Override
    public void render(GuiGraphics context /*? >=1.21.6 {*/ , DeltaTracker tickCounter /*?}*/) {
        try {
            if (tracker == null || tracker.game == null || !events.isInGtb()
                    || !GuessTheUtilsConfig.CONFIG.instance().enableCustomScoreboardModule) return;

            boolean shadow = GuessTheUtilsConfig.CONFIG.instance().customScoreboardTextShadow;
            int lineSpacing = GuessTheUtilsConfig.CONFIG.instance().customScoreboardLineSpacing;
            int linePadding = GuessTheUtilsConfig.CONFIG.instance().customScoreboardLinePadding;
            boolean drawSeparatorBg = GuessTheUtilsConfig.CONFIG.instance().customScoreboardDrawSeparatorBackground;
            int defaultSeparatorHeight = GuessTheUtilsConfig.CONFIG.instance().customScoreboardSeparatorHeight;

            boolean expanded = CLIENT.options.keyPlayerList.isDown();
            Font renderer = Minecraft.getInstance().font;

            boolean includePlaces = GuessTheUtilsConfig.CONFIG.instance().customScoreboardShowPlaces.equals(GuessTheUtilsConfig.CustomScoreboardOption.EXPANDED) ? expanded
                    : GuessTheUtilsConfig.CONFIG.instance().customScoreboardShowPlaces.equals(GuessTheUtilsConfig.CustomScoreboardOption.ON);
            boolean includeTitles = GuessTheUtilsConfig.CONFIG.instance().customScoreboardShowTitles.equals(GuessTheUtilsConfig.CustomScoreboardOption.EXPANDED) ? expanded
                    : GuessTheUtilsConfig.CONFIG.instance().customScoreboardShowTitles.equals(GuessTheUtilsConfig.CustomScoreboardOption.ON);
            boolean includeEmblems = GuessTheUtilsConfig.CONFIG.instance().customScoreboardShowEmblems.equals(GuessTheUtilsConfig.CustomScoreboardOption.EXPANDED) ? expanded
                    : GuessTheUtilsConfig.CONFIG.instance().customScoreboardShowEmblems.equals(GuessTheUtilsConfig.CustomScoreboardOption.ON);
            boolean includePointsGained = GuessTheUtilsConfig.CONFIG.instance().customScoreboardShowPoinsGainedInRound.equals(GuessTheUtilsConfig.CustomScoreboardOption.EXPANDED) ? expanded
                    : GuessTheUtilsConfig.CONFIG.instance().customScoreboardShowPoinsGainedInRound.equals(GuessTheUtilsConfig.CustomScoreboardOption.ON);

            // Technically, round starts when the theme is picked, but I think it's confusing
            int visualCurrentRound = tracker.game.currentRound;
            if (GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE)) visualCurrentRound++;

            List<ScoreboardLine> lines = new ArrayList<>();

            // Round line
            lines.add(new TextLine(
                    List.of(Component.literal("Round").withStyle(textColor)),
                    List.of(),
                    List.of(Component.literal(String.valueOf(visualCurrentRound)).withStyle(accentColor)
                            .append(Component.literal("/").withStyle(textColor))
                            .append(Component.literal(String.valueOf(tracker.game.totalRounds)).withStyle(accentColor)))));

            // Timer line
            String timerState = GameTracker.state.equals(GTBEvents.GameState.ROUND_PRE) ? "Starts In" :
                    GameTracker.state.equals(GTBEvents.GameState.ROUND_BUILD) ? "Time Left" : "Next Round";

            String timer = tracker.game.currentTimer;
            if (timer.isEmpty()) timer = "00:00";

            lines.add(new TextLine(
                    List.of(Component.literal(timerState).withStyle(textColor)),
                    List.of(),
                    List.of(Component.literal(timer.substring(1)).withStyle(accentColor))));

            // Separator
            lines.add(new SeparatorLine(defaultSeparatorHeight));

            // Player Lines
            List<GameTracker.Player> sortedPlayers = List.copyOf(tracker.game.players).stream()
                    .sorted(Comparator.comparingInt(GameTracker.Player::getTotalPoints).reversed()).toList();

            sortedPlayers.forEach(p -> lines.add(new PlayerLine(p)));

            // Separator
            lines.add(new SeparatorLine(defaultSeparatorHeight));

            // Theme title
            lines.add(new TextLine(
                    List.of(),
                    List.of(tracker.game.currentTheme.isEmpty() ? Component.literal("Theme:").withStyle(textColor)
                            : Component.literal("Theme [").withStyle(textColor)
                            .append(Component.literal(getLengthAsString(tracker.game.currentTheme))
                                    .withStyle(accentColor).append(Component.literal("]:").withStyle(textColor)))),
                    List.of()));

            // Theme line
            lines.add(new TextLine(
                    List.of(),
                    List.of(tracker.game.currentTheme.isEmpty() ? Component.literal(unknownThemeString)
                            .withStyle(unknownThemeColor) : Component.literal(tracker.game.currentTheme).withStyle(accentColor)),
                    List.of()));

            // TODO: maybe if the theme length is short enough, single line would work
            AtomicInteger height = new AtomicInteger();

            lines.forEach(l -> height.addAndGet(
                    (l instanceof SeparatorLine ?
                            ((SeparatorLine) l).height() : renderer.lineHeight - 2 + linePadding * 2 + lineSpacing))
            );

            int width = getTotalWidth(renderer, lines, linePadding, includeTitles, includeEmblems, includePointsGained,
                    lineItemSpacing, playerNameRightPad, includePlaces, tracker.game);

            int x = context.guiWidth() - width;
            int y = context.guiHeight() / 2 - height.get() / 2 - heightOffset;

            float backgroundOpacity = GuessTheUtilsConfig.CONFIG.instance().customScoreboardBackgroundOpacity;
            float backgroundHighlightOpacity = GuessTheUtilsConfig.CONFIG.instance().customScoreboardHighlightStrength;

            int bgColor = rgbToArgb(TextColor.fromLegacyFormat(backgroundColor).getValue(), backgroundOpacity);
            int fgColor = rgbToArgb(0xFFFFFF, foregroundOpacity);
            int fgColorInactive = rgbToArgb(0xFFFFFF, foregroundOpacityInactive);
            int fgColorPointsThisRound = rgbToArgb(0xFFFFFF, pointsThisRoundOpacity);
            int backgroundHighlightColor = rgbToArgb(TextColor.fromLegacyFormat(accentColor).getValue(), backgroundHighlightOpacity);
            int backgroundHighlightColorBuilder = rgbToArgb(TextColor.fromLegacyFormat(accentColorBuilder).getValue(), backgroundHighlightOpacity);

            int playerPlace = 1;
            for (ScoreboardLine line : lines) {
                int lineHeight = drawLine(context, renderer, line, x, y, width, linePadding, includeTitles,
                        includeEmblems, includePointsGained, lineItemSpacing, lineSpacing, bgColor, fgColor, textColor,
                        fgColorInactive, fgColorPointsThisRound, accentColor, accentColorBuilder,
                        backgroundHighlightColor, backgroundHighlightColorBuilder, notBuiltIconColor,
                        notBuiltIconOpacity, inactiveIconColor, leaverIconColor, pointsThisRoundColor1, pointsThisRoundColor2,
                        pointsThisRoundColor3, pointsColor, pointsColorHighlight, tracker.game, playerPlace, shadow, drawSeparatorBg, includePlaces);
                if (line instanceof PlayerLine) playerPlace++;
                y += lineHeight;
            }
        } catch (Exception ignored) {
        }
    }

    public interface ScoreboardLine {}
    public record SeparatorLine(int height) implements ScoreboardLine {}
    public record TextLine(List<Component> left, List<Component> center, List<Component> right) implements ScoreboardLine {}
    public record PlayerLine(GameTracker.Player player) implements ScoreboardLine {}
}