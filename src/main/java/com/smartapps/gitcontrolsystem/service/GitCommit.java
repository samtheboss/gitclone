
package com.smartapps.gitcontrolsystem.service;

import com.smartapps.gitcontrolsystem.utils.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class GitCommit {
    // Method to commit changes with a given message
    public static void commit(String message) {
        String author = "test"; // Author of the commit
        try {
            String branchName = GitServices.getCurrentBranch(); // Get the current branch name
            if (branchName.isEmpty()) { // Check if the branch name is empty
                System.out.println("No active branch found. Please create or switch to a branch.");
                return; // Exit if no active branch
            }

            String parentCommitHash = Utils.getBranchCommit(branchName); // Get the parent commit hash
            String commitHash = generateCommitHash(); // Generate a unique commit hash
            File commitFile = new File(".dotgit/commits/" + commitHash); // Create a new commit file
            commitFile.getParentFile().mkdirs(); // Create directories for the commit file if they do not exist
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(commitFile))) {
                // Write commit details to the file
                writer.write("Commit: " + commitHash + "\n");
                writer.write("Author: " + author + "\n");
                writer.write("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                writer.write("Message: " + message + "\n");
                if (parentCommitHash != null) { // Check if there is a parent commit
                    writer.write("Parent Commit: " + parentCommitHash + "\n");
                }
            }

            File branchFile = new File(".dotgit/refs/heads/" + branchName); // Reference to the current branch file
            Files.write(branchFile.toPath(), commitHash.getBytes(), StandardOpenOption.CREATE); // Update branch reference
            System.out.println("Commit successful! Hash: " + commitHash); // Confirmation message

        } catch (IOException e) {
            e.printStackTrace(); // Print stack trace for any IO exceptions
            System.out.println("An error occurred while committing."); // Error message
        }
    }

    // Method to generate a unique commit hash
    public static String generateCommitHash() {
        long timestamp = System.currentTimeMillis(); // Get the current timestamp
        int randomValue = new java.util.Random().nextInt(); // Generate a random integer
        String base = timestamp + "-" + randomValue; // Create a base string
        return Integer.toHexString(base.hashCode()); // Return the hash as a hexadecimal string
    }

    // Method to retrieve staged files
    private static List<String> getStagedFiles() throws IOException {
        File indexFile = new File(".dotgit/index"); // Reference to the index file
        if (!indexFile.exists()) { // Check if the index file exists
            return new ArrayList<>(); // Return an empty list if it does not exist
        }
        List<String> stagedFiles = new ArrayList<>(); // List to hold staged files
        try (BufferedReader reader = new BufferedReader(new FileReader(indexFile))) {
            String line;
            while ((line = reader.readLine()) != null) { // Read each line from the index file
                stagedFiles.add(line.split(" ")[0]); // Add the filename to the staged files list
            }
        }
        return stagedFiles; // Return the list of staged files
    }

    // Get the parent commit for the current branch
    public static String getParentCommit(String currentBranch) {
        File branchRefFile = new File(".dotgit/refs/heads/" + currentBranch); // Reference to the branch file
        if (!branchRefFile.exists()) { // Check if the branch file exists
            return null; // Return null if it does not exist
        }

        try {
            return new String(Files.readAllBytes(branchRefFile.toPath())).trim(); // Read and return the parent commit hash
        } catch (IOException e) {
            e.printStackTrace(); // Print stack trace for any IO exceptions
            return null; // Return null in case of an exception
        }
    }

    // Save the commit to the .dotgit/commits directory
    private static void saveCommit(String commitHash, String commitMessage) throws IOException {
        File commitDir = new File(".dotgit/commits"); // Reference to the commits directory
        if (!commitDir.exists()) { // Check if the commits directory exists
            commitDir.mkdirs(); // Create the directory if it does not exist
        }

        File commitFile = new File(commitDir, commitHash); // Create a new commit file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(commitFile))) {
            writer.write(commitMessage); // Write the commit message to the file
        }
    }

    // Update the current branch to point to the new commit
    private static void updateBranchReference(String currentBranch, String commitHash) throws IOException {
        File branchRefFile = new File(".dotgit/refs/heads/" + currentBranch); // Reference to the branch file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(branchRefFile))) {
            writer.write(commitHash); // Write the new commit hash to the branch file
        }
    }

    // Clear staged files after commit
    private static void clearStagedFiles() {
        File indexFile = new File(".dotgit/index"); // Reference to the index file
        if (indexFile.exists()) { // Check if the index file exists
            indexFile.delete(); // Delete the index file
        }
    }

    // Method to view commit history
    public static String viewCommitHistory() {
        StringBuilder history = new StringBuilder(""); // StringBuilder to hold commit history
        File commitsDir = new File(".dotgit/commits"); // Reference to the commits directory
        if (!commitsDir.exists() || !commitsDir.isDirectory()) { // Check if the commits directory exists
            System.out.println("No commits found."); // Message if no commits are found
            return null; // Return null
        }
        try {
            File[] commitFiles = commitsDir.listFiles(); // List all commit files
            if (commitFiles != null && commitFiles.length > 0) { // Check if there are any commit files
                List<String> commitHistory = new ArrayList<>(); // List to hold commit history
                for (File commitFile : commitFiles) { // Iterate through each commit file
                    String commitHash = commitFile.getName(); // Get the commit hash from the file name
                    String commitMessage = new String(Files.readAllBytes(commitFile.toPath())); // Read the commit message
                    commitHistory.add("Commit Hash: " + commitHash + "\n" + commitMessage); // Add to commit history
                }
                // Print out commit history (reverse chronological order)
                Collections.reverse(commitHistory); // Reverse the order of commit history
                for (String commit : commitHistory) { // Iterate through the commit history
                    history.append(commit).append("\n"); // Append each commit to the history
                    System.out.println(commit); // Print each commit
                    System.out.println("-------------------"); // Separator for readability
                }
            } else {
                System.out.println("No commits found."); // Message if no commits are found
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print stack trace for any IO exceptions
        }
        return history.toString(); // Return the commit history as a string
    }

    // Method to read commit files
    public static Map<String, String> readCommitFiles(String commitHash) throws IOException {
        Map<String, String> files = new HashMap<>(); // Map to hold filename and hash
        File commitFile = new File(".dotgit/objects/" + commitHash); // Reference to the commit file

        if (!commitFile.exists()) { // Check if the commit file exists
            System.err.println("Commit " + commitHash + " does not exist."); // Error message
            return files; // Return empty map
        }

        List<String> lines = Files.readAllLines(commitFile.toPath(), StandardCharsets.UTF_8); // Read lines from the commit file

        for (String line : lines) { // Iterate through each line
            if (!line.startsWith("message:") && !line.startsWith("parent:")) { // Exclude message and parent lines
                String[] parts = line.split(" "); // Split the line into parts
                if (parts.length == 2) { // Check if there are two parts
                    files.put(parts[0], parts[1]); // Add filename and hash to the map
                }
            }
        }

        return files; // Return the map of files
    }

    // Method to commit with parent hashes
    public static void commitWithParents(String message, String... parentHashes) throws IOException {
        File indexFile = new File(".dotgit/index"); // Reference to the index file
        if (!indexFile.exists() || indexFile.length() == 0) { // Check if the index file exists and is not empty
            System.out.println("No changes to commit."); // Message if there are no changes
            return; // Exit if no changes
        }

        Map<String, String> stagedFiles = new HashMap<>(); // Map to hold staged files
        List<String> indexEntries = Files.readAllLines(indexFile.toPath(), StandardCharsets.UTF_8); // Read index entries

        for (String entry : indexEntries) { // Iterate through each entry
            String[] parts = entry.split(" "); // Split the entry into parts
            if (parts.length == 2) { // Check if there are two parts
                stagedFiles.put(parts[0], parts[1]); // Add filename and hash to the staged files map
            }
        }
        StringBuilder commitContent = new StringBuilder(); // StringBuilder to hold commit content
        commitContent.append("message: ").append(message).append("\n"); // Append the commit message
        if (parentHashes != null && parentHashes.length > 0) { // Check if there are parent hashes
            for (String parentHash : parentHashes) { // Iterate through each parent hash
                commitContent.append("parent: ").append(parentHash).append("\n"); // Append parent hash
            }
        }
        for (Map.Entry<String, String> entry : stagedFiles.entrySet()) { // Iterate through staged files
            commitContent.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n"); // Append filename and hash
        }
        String commitHash = Utils.computeHash(commitContent.toString()); // Compute the commit hash
        File commitFile = new File(".dotgit/objects/" + commitHash); // Reference to the commit file
        Files.writeString(commitFile.toPath(), commitContent.toString()); // Write commit content to the file
        File headFile = new File(".dotgit/HEAD"); // Reference to the HEAD file
        String branchName = Utils.readFile(headFile).replace("ref: refs/heads/", "").trim(); // Get the current branch name
        File branchFile = new File(".dotgit/refs/heads/" + branchName); // Reference to the branch file
        Files.writeString(branchFile.toPath(), commitHash); // Update the branch file with the new commit hash

        Files.delete(indexFile.toPath()); // Delete the index file after commit
        System.out.println("Committed with hash: " + commitHash); // Confirmation message
    }
}
