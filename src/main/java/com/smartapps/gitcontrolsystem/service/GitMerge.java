
package com.smartapps.gitcontrolsystem.service;

import com.smartapps.gitcontrolsystem.utils.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

public class GitMerge {
    // Method to merge two branches
    public static void merge(String targetBranch, String currentBranch, String author) {
        try {
            // Get the latest commit hashes for the branches
            String targetBranchCommit = Utils.getBranchCommit(targetBranch);
            String currentBranchCommit = Utils.getBranchCommit(currentBranch);

            // Check if either branch has no commits
            if (targetBranchCommit == null || currentBranchCommit == null) {
                System.out.println("Cannot merge. One or both branches have no commits.");
                return; // Exit the method if no commits are found
            }

            // Identify the base commit (common ancestor)
            String baseCommit = findCommonAncestor(targetBranchCommit, currentBranchCommit);
            // Check if a common ancestor was found
            if (baseCommit == null) {
                System.out.println("No common ancestor found. Cannot perform a merge.");
                return; // Exit the method if no common ancestor is found
            }

            // Collect changes from both branches
            Map<String, String> baseChanges = readCommitFiles(baseCommit);
            Map<String, String> currentChanges = readCommitFiles(currentBranchCommit);
            Map<String, String> targetChanges = readCommitFiles(targetBranchCommit);

            // Perform a three-way merge
            Map<String, String> mergedChanges = new HashMap<>();
            // Iterate through files in the base changes
            for (String file : baseChanges.keySet()) {
                String baseContent = baseChanges.get(file); // Get base content
                String currentContent = currentChanges.getOrDefault(file, baseContent); // Get current content or base if not present
                String targetContent = targetChanges.getOrDefault(file, baseContent); // Get target content or base if not present

                // Check for conflicts and determine which content to keep
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
                    // Mark the conflict in the merged changes
                    mergedChanges.put(file, "<<<<<<< CURRENT\n" + currentContent + "\n=======\n" + targetContent + "\n>>>>>>>");
                }
            }

            // Apply merged changes to the files
            applyMergedChanges(mergedChanges);

            // Create a merge commit with the provided author and message
            createMergeCommit(author, "Merged branch '" + targetBranch + "' into '" + currentBranch + "'", currentBranchCommit, targetBranchCommit);

            System.out.println("Merge completed successfully.");
        } catch (Exception e) {
            e.printStackTrace(); // Print stack trace for debugging
            System.out.println("An error occurred during the merge.");
        }
    }

    // Method to find the common ancestor of two commits
    private static String findCommonAncestor(String commit1, String commit2) throws IOException {
        Set<String> visited = new HashSet<>(); // Set to track visited commits
        while (commit1 != null || commit2 != null) {
            if (commit1 != null) {
                // Check if commit1 has been visited
                if (!visited.add(commit1)) {
                    return commit1; // Found common ancestor
                }
                commit1 = GitCommit.getParentCommit(commit1); // Move to parent commit
            }
            if (commit2 != null) {
                // Check if commit2 has been visited
                if (!visited.add(commit2)) {
                    return commit2; // Found common ancestor
                }
                commit2 = GitCommit.getParentCommit(commit2); // Move to parent commit
            }
        }
        return null; // No common ancestor found
    }

    // Method to read files associated with a specific commit
    private static Map<String, String> readCommitFiles(String commitHash) throws IOException {
        Map<String, String> files = new HashMap<>(); // Map to store file names and their contents
        File commitDir = new File(".dotgit/commits/" + commitHash + "_files"); // Directory for commit files
        if (commitDir.exists()) {
            // Iterate through files in the commit directory
            for (File file : Objects.requireNonNull(commitDir.listFiles())) {
                files.put(file.getName(), new String(Files.readAllBytes(file.toPath()))); // Read file content
            }
        }
        return files; // Return the map of files
    }

    // Method to apply merged changes to the file system
    private static void applyMergedChanges(Map<String, String> mergedChanges) throws IOException {
        // Iterate through the merged changes
        for (Map.Entry<String, String> entry : mergedChanges.entrySet()) {
            String fileName = entry.getKey(); // Get file name
            String fileContent = entry.getValue(); // Get file content

            File file = new File(fileName); // Create a file object
            file.getParentFile().mkdirs(); // Create parent directories if they do not exist
            // Write the merged content to the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(fileContent);
            }
        }
    }

    // Method to create a merge commit
    private static void createMergeCommit(String author, String message, String currentCommit, String mergedCommit) throws IOException {
        String commitHash = GitCommit.generateCommitHash(); // Generate a new commit hash
        File commitFile = new File(".dotgit/commits/" + commitHash); // Create a file for the commit
        commitFile.getParentFile().mkdirs(); // Create parent directories if they do not exist

        // Write commit details to the commit file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(commitFile))) {
            writer.write("Commit: " + commitHash + "\n");
            writer.write("Author: " + author + "\n");
            writer.write("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            writer.write("Message: " + message + "\n");
            writer.write("Parent Commit 1: " + currentCommit + "\n");
            writer.write("Parent Commit 2: " + mergedCommit + "\n");
        }

        // Update the branch reference to point to the new commit
        String currentBranch = GitServices.getCurrentBranch(); // Get the current branch name
        File branchFile = new File(".dotgit/refs/heads/" + currentBranch); // File for the branch reference
        Files.write(branchFile.toPath(), commitHash.getBytes(), StandardOpenOption.CREATE); // Write the new commit hash to the branch file
    }
}
