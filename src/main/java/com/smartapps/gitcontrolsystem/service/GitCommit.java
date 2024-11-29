package com.smartapps.gitcontrolsystem.service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class GitCommit {

    public static void commit(String commitMessage) {
        String currentBranch = GitServices.getCurrentBranch();
        try {
            List<String> stagedFiles = getStagedFiles();
            if (stagedFiles.isEmpty()) {
                System.out.println("No files staged for commit.");
                return;
            }
            String parentCommit = getParentCommit(currentBranch);
            String commitHash = UUID.randomUUID().toString();  // Unique commit hash
            String commitMessageFormatted = "Commit Hash: " + commitHash + "\n"
                    + "Parent Commit: " + parentCommit + "\n"
                    + "Message: " + commitMessage + "\n"
                    + "Date: " + new Date() + "\n"
                    + "Files Staged: " + String.join(", ", stagedFiles);

            // Save the commit to the commits directory
            saveCommit(commitHash, commitMessageFormatted);

            // Update the current branch to point to the new commit
            updateBranchReference(currentBranch, commitHash);

            // Clear the staged files after commit
            clearStagedFiles();

            System.out.println("Commit successful: " + commitMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get the current branch
    // Get the list of staged files
    private static List<String> getStagedFiles() throws IOException {
        File indexFile = new File(".dotgit/index");
        if (!indexFile.exists()) {
            return new ArrayList<>();
        }

        List<String> stagedFiles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(indexFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stagedFiles.add(line.split(" ")[0]); // Just get the file name (not the hash)
            }
        }

        return stagedFiles;
    }

    // Get the parent commit for the current branch
    private static String getParentCommit(String currentBranch) throws IOException {
        File branchRefFile = new File(".dotgit/refs/heads/" + currentBranch);
        if (!branchRefFile.exists()) {
            return null;
        }

        try {
            return new String(Files.readAllBytes(branchRefFile.toPath())).trim();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Save the commit to the .dotgit/commits directory
    private static void saveCommit(String commitHash, String commitMessage) throws IOException {
        File commitDir = new File(".dotgit/commits");
        if (!commitDir.exists()) {
            commitDir.mkdirs();
        }

        File commitFile = new File(commitDir, commitHash);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(commitFile))) {
            writer.write(commitMessage);
        }
    }

    // Update the current branch to point to the new commit
    private static void updateBranchReference(String currentBranch, String commitHash) throws IOException {
        File branchRefFile = new File(".dotgit/refs/heads/" + currentBranch);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(branchRefFile))) {
            writer.write(commitHash);
        }
    }

    // Clear staged files after commit
    private static void clearStagedFiles() throws IOException {
        File indexFile = new File(".dotgit/index");
        if (indexFile.exists()) {
            indexFile.delete();
        }
    }
    public static String viewCommitHistory() {
        StringBuilder history = new StringBuilder("");
        File commitsDir = new File(".dotgit/commits");
        if (!commitsDir.exists() || !commitsDir.isDirectory()) {
            System.out.println("No commits found.");
            return null;
        }
        try {
            File[] commitFiles = commitsDir.listFiles();
            if (commitFiles != null && commitFiles.length > 0) {
                List<String> commitHistory = new ArrayList<>();
                for (File commitFile : commitFiles) {
                    String commitHash = commitFile.getName();
                    String commitMessage = new String(Files.readAllBytes(commitFile.toPath()));
                    commitHistory.add("Commit Hash: " + commitHash + "\n" + commitMessage);
                }
                // Print out commit history (reverse chronological order)
                Collections.reverse(commitHistory);
                for (String commit : commitHistory) {
                    history.append(commit).append("\n");
                    System.out.println(commit);
                    System.out.println("-------------------");
                }
            } else {
                System.out.println("No commits found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return history.toString();
    }
}
