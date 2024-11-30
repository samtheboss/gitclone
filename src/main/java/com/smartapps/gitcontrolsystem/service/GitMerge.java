package com.smartapps.gitcontrolsystem.service;

import com.smartapps.gitcontrolsystem.utils.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

public class GitMerge {

    public static void merge(String targetBranch, String currentBranch, String author) {
        try {
            // Get the latest commit hashes for the branches
            String targetBranchCommit = Utils.getBranchCommit(targetBranch);
            String currentBranchCommit = Utils.getBranchCommit(currentBranch);

            if (targetBranchCommit == null || currentBranchCommit == null) {
                System.out.println("Cannot merge. One or both branches have no commits.");
                return;
            }

            // Identify the base commit (common ancestor)
            String baseCommit = findCommonAncestor(targetBranchCommit, currentBranchCommit);
            if (baseCommit == null) {
                System.out.println("No common ancestor found. Cannot perform a merge.");
                return;
            }

            // Collect changes from both branches
            Map<String, String> baseChanges = readCommitFiles(baseCommit);
            Map<String, String> currentChanges = readCommitFiles(currentBranchCommit);
            Map<String, String> targetChanges = readCommitFiles(targetBranchCommit);

            // Perform a three-way merge
            Map<String, String> mergedChanges = new HashMap<>();
            for (String file : baseChanges.keySet()) {
                String baseContent = baseChanges.get(file);
                String currentContent = currentChanges.getOrDefault(file, baseContent);
                String targetContent = targetChanges.getOrDefault(file, baseContent);

                if (currentContent.equals(targetContent)) {
                    // No conflict, keep the current content
                    mergedChanges.put(file, currentContent);
                } else if (currentContent.equals(baseContent)) {
                    // No conflict, accept target changes
                    mergedChanges.put(file, targetContent);
                } else if (targetContent.equals(baseContent)) {
                    // No conflict, accept current changes
                    mergedChanges.put(file, currentContent);
                } else {
                    // Conflict detected
                    System.out.println("Conflict detected in file: " + file);
                    mergedChanges.put(file, "<<<<<<< CURRENT\n" + currentContent + "\n=======\n" + targetContent + "\n>>>>>>>");
                }
            }

            // Apply merged changes
            applyMergedChanges(mergedChanges);

            // Create a merge commit
            createMergeCommit(author, "Merged branch '" + targetBranch + "' into '" + currentBranch + "'", currentBranchCommit, targetBranchCommit);

            System.out.println("Merge completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("An error occurred during the merge.");
        }
    }

    private static String findCommonAncestor(String commit1, String commit2) throws IOException {
        Set<String> visited = new HashSet<>();
        while (commit1 != null || commit2 != null) {
            if (commit1 != null) {
                if (!visited.add(commit1)) {
                    return commit1; // Found common ancestor
                }
                commit1 = GitCommit.getParentCommit(commit1);
            }
            if (commit2 != null) {
                if (!visited.add(commit2)) {
                    return commit2; // Found common ancestor
                }
                commit2 = GitCommit.getParentCommit(commit2);
            }
        }
        return null; // No common ancestor found
    }

    private static Map<String, String> readCommitFiles(String commitHash) throws IOException {
        Map<String, String> files = new HashMap<>();
        File commitDir = new File(".dotgit/commits/" + commitHash + "_files");
        if (commitDir.exists()) {
            for (File file : Objects.requireNonNull(commitDir.listFiles())) {
                files.put(file.getName(), new String(Files.readAllBytes(file.toPath())));
            }
        }
        return files;
    }

    private static void applyMergedChanges(Map<String, String> mergedChanges) throws IOException {
        for (Map.Entry<String, String> entry : mergedChanges.entrySet()) {
            String fileName = entry.getKey();
            String fileContent = entry.getValue();

            File file = new File(fileName);
            file.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(fileContent);
            }
        }
    }

    private static void createMergeCommit(String author, String message, String currentCommit, String mergedCommit) throws IOException {
        String commitHash = GitCommit.generateCommitHash();
        File commitFile = new File(".dotgit/commits/" + commitHash);
        commitFile.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(commitFile))) {
            writer.write("Commit: " + commitHash + "\n");
            writer.write("Author: " + author + "\n");
            writer.write("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            writer.write("Message: " + message + "\n");
            writer.write("Parent Commit 1: " + currentCommit + "\n");
            writer.write("Parent Commit 2: " + mergedCommit + "\n");
        }

        // Update the branch reference
        String currentBranch = GitServices.getCurrentBranch();
        File branchFile = new File(".dotgit/refs/heads/" + currentBranch);
        Files.write(branchFile.toPath(), commitHash.getBytes(), StandardOpenOption.CREATE);
    }
}
