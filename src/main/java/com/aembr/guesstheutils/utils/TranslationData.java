package com.aembr.guesstheutils.utils;

import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class TranslationData {
    private static final String GITHUB_BASE_URL = "https://raw.githubusercontent.com/zmh-program/gtb_platform/refs/heads/main/lib/source/";
    private static final String LAST_CHECKED_FILENAME = "translation-data-last-checked";
    private static final String LAST_UPDATED_FILENAME = "versions.json";
    private static final String TRANSLATIONS_DATA_FILENAME = "translations-data.json";

    public static List<TranslationDataEntry> entries;

    public static void init() {
        try {
            String lastCheckedDate = FileUtils.readFile(LAST_CHECKED_FILENAME);
            if (!lastCheckedDate.equals(LocalDate.now().toString())) {
                GuessTheUtils.LOGGER.info("Checking for translation data updates...");
                String lastUpdatedContent = FileUtils.downloadFile(GITHUB_BASE_URL + LAST_UPDATED_FILENAME);
                String localLastUpdatedContent = FileUtils.readFile(LAST_UPDATED_FILENAME);

                if (!lastUpdatedContent.equals(localLastUpdatedContent)) {
                    GuessTheUtils.LOGGER.info("Translation data update available! Downloading...");
                    FileUtils.writeFile(LAST_UPDATED_FILENAME, lastUpdatedContent);
                    String shortcutsContent = FileUtils.downloadFile(GITHUB_BASE_URL + TRANSLATIONS_DATA_FILENAME);
                    FileUtils.writeFile(TRANSLATIONS_DATA_FILENAME, shortcutsContent);
                }

                FileUtils.writeFile(LAST_CHECKED_FILENAME, LocalDate.now().toString());
            } else {
                GuessTheUtils.LOGGER.info("Skipping translation data update check, as we " +
                        "already checked today");
            }

            entries = loadData(TRANSLATIONS_DATA_FILENAME);
            GuessTheUtils.LOGGER.info("Translation data loaded!");

        } catch (IOException e) {
            GuessTheUtils.LOGGER.error(e.getMessage());
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static List<TranslationDataEntry> loadData(String filename) {
        File configFile = new File(GuessTheUtilsConfig.CONFIG_PATH.toFile(), filename);
        Gson gson = new Gson();
        Type translationDataType = new TypeToken<List<TranslationDataEntry>>() {}.getType();

        try (FileReader reader = new FileReader(configFile, StandardCharSets.UTF_8)) {
            return gson.fromJson(reader, translationDataType);
        } catch (IOException e) {
            GuessTheUtils.LOGGER.error(e.getMessage());
            return List.of();
        }
    }

    public record TranslationDataEntry(
            int id,
            String theme,
            String shortcut,
            List<Multiword> multiwords,
            Map<String, Translation> translations
    ) {
        @Override
        public @NotNull String toString() {
            return "TranslationDataEntry{" +
                    "id=" + id +
                    ", theme='" + theme + '\'' +
                    ", shortcut='" + shortcut + '\'' +
                    ", multiwords=" + multiwords +
                    ", translations=" + translations +
                    '}';
        }
    }

    public record Multiword(
            String multiword,
            List<Occurrence> occurrences
    ) {

        @Override
        public @NotNull String toString() {
            return "Multiword{" +
                    "multiword='" + multiword + '\'' +
                    ", occurrences=" + occurrences +
                    '}';
        }
    }

    public record Occurrence(
            String theme,
            String reference
    ) {
        @Override
        public @NotNull String toString() {
            return "Occurrence{" +
                    "theme='" + theme + '\'' +
                    ", reference='" + reference + '\'' +
                    '}';
        }
    }

    public record Translation(
            String translation,
            @SerializedName("is_approved") boolean isApproved,
            @SerializedName("approved_at") String approvedAt
    ) {
        @Override
        public @NotNull String toString() {
            return "Translation{" +
                    "translation='" + translation + '\'' +
                    ", isApproved=" + isApproved +
                    ", approvedAt='" + approvedAt + '\'' +
                    '}';
        }
    }
}
