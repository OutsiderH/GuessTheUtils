package com.aembr.guesstheutils.utils;

import com.aembr.guesstheutils.config.GuessTheUtilsConfig;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileUtils {
    static String readFile(String filename) throws IOException {
        File file = new File(GuessTheUtilsConfig.CONFIG_PATH.toFile(), filename);
        return file.exists() ? new String(Files.readAllBytes(file.toPath())).trim() : "";
    }

    static void writeFile(String filename, String content) throws IOException {
        Files.write(new File(GuessTheUtilsConfig.CONFIG_PATH.toFile(), filename).toPath(), content.getBytes());
    }

    @SuppressWarnings("SameParameterValue")
    static String downloadFile(String fileUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to download file: " + fileUrl);
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharSets.UTF_8));
             StringWriter writer = new StringWriter()) {
            String line;
            while ((line = in.readLine()) != null) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
            return writer.toString().trim();
        }
    }
}
