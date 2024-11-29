package com.smartapps.gitcontrolsystem.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Instant;

public class Utils {
    public static void writeToFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    public static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    public static String computeCommitHash(String stagedFiles, String parentCommit, String message)
    {
        return String.valueOf((stagedFiles + parentCommit + message + Instant.now().toEpochMilli()).hashCode());
    }
    public static void appendToFile(File file, String content) throws IOException {
        FileWriter writer = new FileWriter(file, true); // Open file in append mode
        writer.write(content); // Write the content
        writer.close(); // Close the writer
    }
    public static String computeHash(String content) {
        return String.valueOf(content.hashCode());
    }
}
