package com.aembr.guesstheutils.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class Utils {
    public static List<Component> getScoreboardLines(Minecraft client) {
        HashMap<Component, Integer> scoredLines = new HashMap<>();
        LocalPlayer player = client.player;
        if (player == null) return new ArrayList<>();

        Scoreboard scoreboard = player.connection.scoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BY_ID.apply(1));

        for (ScoreHolder scoreHolder : scoreboard.getTrackedPlayers()) {
            if (!scoreboard.listPlayerScores(scoreHolder).containsKey(objective)) continue;

            PlayerTeam team = scoreboard.getPlayersTeam(scoreHolder.getScoreboardName());
            if (team == null) continue;

            Component textLine = Component.empty().append(team.getPlayerPrefix().copy()).append(team.getPlayerSuffix().copy());
            String strLine = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();

            if (strLine.trim().isEmpty()) continue;

            int teamScore = Objects.requireNonNull(scoreboard.getPlayerScoreInfo(scoreHolder, objective)).value();
            scoredLines.put(textLine, teamScore);
        }
        // The objective name is usually animated, and the formatting doesn't convey any info, so we strip it
        if (objective != null) scoredLines.put(Component.nullToEmpty(ChatFormatting.stripFormatting(objective.getDisplayName().getString())), Integer.MAX_VALUE);

        return scoredLines.entrySet().stream().sorted((e1, e2) ->
                Integer.compare(e2.getValue(), e1.getValue())).map(Map.Entry::getKey).toList();
    }

    public static List<Component> collectTabListEntries(Minecraft client) {
        if (client.player == null) return new ArrayList<>();

        return client.level.players().stream().map(entry -> {
            MutableComponent entryText;
            if (entry.getDisplayName() != null) {
                entryText = entry.getDisplayName().copy();
            } else {
                entryText = entry.getName().copy();
            }
            return (Component) entryText;
        }).toList();
    }

    public static class FixedSizeBuffer<T> {
        private final int maxSize;
        private final List<T> buffer;

        public FixedSizeBuffer(int maxSize) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException("Max size must be greater than 0");
            }
            this.maxSize = maxSize;
            this.buffer = new ArrayList<>(maxSize);
        }

        @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
        public void add(T element) {
            buffer.add(0, element);
            if (buffer.size() > maxSize) {
                buffer.remove(buffer.size() - 1);
            }
        }

        public T get(int index) {
            if (index < 0 || index >= buffer.size()) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + buffer.size());
            }
            return buffer.get(index);
        }

        public int size() {
            return buffer.size();
        }
    }

    public record Pair<T1, T2>(T1 a, T2 b) {
        @Override
        public @NotNull String toString() {
            return "Pair{" + this.a() + ", " + this.b() + "}";
        }
    }

    public static String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
