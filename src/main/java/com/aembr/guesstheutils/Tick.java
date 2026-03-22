package com.aembr.guesstheutils;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Tick {
    public List<Component> scoreboardLines;
    public List<Component> playerListEntries;
    public List<Component> chatMessages;
    public Component actionBarMessage;
    public Component title;
    public Component subtitle;
    public Component screenTitle;
    public String error;

    public Tick() {}

    public Tick(JsonObject json) {
        if (json.has("scoreboardLines")) {
            scoreboardLines = deserializeList(json.get("scoreboardLines"));
        }
        if (json.has("playerListEntries")) {
            playerListEntries = deserializeList(json.get("playerListEntries"));
        }
        if (json.has("chatMessages")) {
            chatMessages = deserializeList(json.get("chatMessages"));
        }
        if (json.has("actionBarMessage")) {
            actionBarMessage = deserializeText(json.get("actionBarMessage").getAsString());
        }
        if (json.has("title")) {
            title = deserializeText(json.get("title").getAsString());
        }
        if (json.has("subtitle")) {
            subtitle = deserializeText(json.get("subtitle").getAsString());
        }
        if (json.has("screenTitle")) {
            screenTitle = deserializeText(json.get("screenTitle").getAsString());
        }
    }

    private List<Component> deserializeList(JsonElement jsonElement) {
        List<Component> textList = new ArrayList<>();
        for (JsonElement element : jsonElement.getAsJsonArray()) {
            String jsonString = element.getAsString();
            textList.add(deserializeText(jsonString));
        }
        return textList;
    }

    private Component deserializeText(String jsonString) {
        return ComponentSerialization.CODEC
                .decode(JsonOps.INSTANCE, new Gson().fromJson(jsonString, JsonElement.class))
                .getOrThrow()
                .getFirst();
    }

    public static List<String> serializeList(List<Component> input) {
        Gson gson = new Gson();
        return input.stream().map(text -> {
            var result = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, text);
            return result
                .map(json -> gson.toJson(json))
                .result()
                .orElseGet(() -> gson.toJson(JsonNull.INSTANCE));
        }).toList();
    }

    public SerializedTick serialize() {
        Gson gson = new Gson();
        return new SerializedTick(scoreboardLines == null ? null : serializeList(scoreboardLines),
                playerListEntries == null ? null : serializeList(playerListEntries),
                chatMessages == null ? null : serializeList(chatMessages),
                actionBarMessage == null ? null : gson.toJson(ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, actionBarMessage).getOrThrow()),
                title == null ? null : gson.toJson(ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, title).getOrThrow()),
                subtitle == null ? null : gson.toJson(ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, subtitle).getOrThrow()),
                screenTitle == null ? null : gson.toJson(ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, screenTitle).getOrThrow()),
                error);
    }

    public boolean isEmpty() {
        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                Object value = field.get(this);
                if (value != null) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                GuessTheUtils.LOGGER.error(e.toString());
            }
        }
        return true;
    }

    public record SerializedTick(List<String> scoreboardLines, List<String> playerListEntries,
                                 List<String> chatMessages, String actionBarMessage, String title, String subtitle,
                                 String screenTitle, String error) {

        @Override
        public @NotNull String toString() {
            return "SerializedTick{" +
                    "scoreboardLines=" + scoreboardLines +
                    ", playerListEntries=" + playerListEntries +
                    ", chatMessages=" + chatMessages +
                    ", actionBarMessage='" + actionBarMessage + '\'' +
                    ", title='" + title + '\'' +
                    ", subtitle='" + subtitle + '\'' +
                    ", screenTitle='" + screenTitle + '\'' +
                    ", error='" + error + '\'' +
                    '}';
        }
    }
}
