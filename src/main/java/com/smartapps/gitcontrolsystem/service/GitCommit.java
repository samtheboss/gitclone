package com.smartapps.gitcontrolsystem.service;

import com.smartapps.gitcontrolsystem.utils.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;



public class GitCommit {

//    public static void commit(String commitMessage) {
//        String currentBranch = GitServices.getCurrentBranch();
//        try {
//            List<String> stagedFiles = getStagedFiles();
//            if (stagedFiles.isEmpty()) {
//                System.out.println("No files staged for commit.");
//                return;
//            }
//            String parentCommit = getParentCommit(currentBranch);
//            String commitHash = UUID.randomUUID().toString();  // Unique commit hash
//            String commitMessageFormatted = "Commit Hash: " + commitHash + "\n"
//                    + "Parent Commit: " + parentCommit + "\n"
//                    + "Message: " + commitMessage + "\n"
//                    + "Date: " + new Date() + "\n"
//                    + "Files Staged: " + String.join(", ", stagedFiles);
//
//            // Save the commit to the commits directory
//            saveCommit(commitHash, commitMessageFormatted);
//
//            // Update the current branch to point to the new commit
//            updateBranchReference(currentBranch, commitHash);
//
//            // Clear the staged files after commit
//            clearStagedFiles();
//
//            System.out.println("Commit successful: " + commitMessage);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    // Get the current branch
    // Get the list of staged files

//    public static void commit(String message) {
//        // Ensure `.dotgit` structure exists
//        try {
//
//
//            String branchName = GitServices.getCurrentBranch();
//            File refsDir = new File(".dotgit/refs/heads");
//            File branchFile = new File(refsDir, branchName);
//
//            if (!branchFile.exists()) {
//                System.out.println("Branch '" + branchName + "' does not exist. Creating it...");
//                branchFile.getParentFile().mkdirs();
//                branchFile.createNewFile();
//            }
//
//            // Read the current HEAD of the branch
//            String parentHash = "";
//            if (branchFile.length() > 0) {
//                parentHash = Files.readString(branchFile.toPath()).trim();
//            }
//
//            // Read the staging area (index) to create the commit
//            File indexFile = new File(".dotgit/index");
//            if (!indexFile.exists() || indexFile.length() == 0) {
//                System.out.println("Nothing to commit.");
//                return;
//            }
//            String indexContent = Files.readString(indexFile.toPath());
//
//            // Generate a commit hash and content
//            String commitContent = "Parent: " + parentHash + "\n" +
//                    "Message: " + message + "\n" +
//                    "Files:\n" + indexContent;
//            String commitHash = Utils.computeHash(commitContent);
//
//            // Write the commit object to the objects directory
//            File commitFile = new File(".dotgit/objects/" + commitHash);
//            commitFile.getParentFile().mkdirs();
//            Files.writeString(commitFile.toPath(), commitContent);
//
//            // Update the branch reference to point to the new commit
//            Files.writeString(branchFile.toPath(), commitHash);
//
//            // Clear the staging area (index)
//            Files.writeString(indexFile.toPath(), "");
//
//            System.out.println("Committed to branch '" + branchName + "' with hash: " + commitHash);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
public static void commit( String message) {
    String author ="test";
    try {
        String branchName = GitServices.getCurrentBranch();
        if (branchName == null || branchName.isEmpty()) {
            System.out.println("No active branch found. Please create or switch to a branch.");
            return;
        }

        String parentCommitHash =Utils.getBranchCommit(branchName);
        String commitHash = generateCommitHash();
        File commitFile = new File(".dotgit/commits/" + commitHash);
        commitFile.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(commitFile))) {
            writer.write("Commit: " + commitHash + "\n");
            writer.write("Author: " + author + "\n");
            writer.write("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            writer.write("Message: " + message + "\n");
            if (parentCommitHash != null) {
                writer.write("Parent Commit: " + parentCommitHash + "\n");
            }
        }

        // Update the branch to point to the new commit
        File branchFile = new File(".dotgit/refs/heads/" + branchName);
        Files.write(branchFile.toPath(), commitHash.getBytes(), StandardOpenOption.CREATE);

        System.out.println("Commit successful! Hash: " + commitHash);

    } catch (IOException e) {
        e.printStackTrace();
        System.out.println("An error occurred while committing.");
    }
}

    public static String generateCommitHash() {
        long timestamp = System.currentTimeMillis(); // Get the current timestamp
        int randomValue = new java.util.Random().nextInt(); // Generate a random value
        String base = timestamp + "-" + randomValue; // Combine for uniqueness

        // Create a hash using a built-in method
        return Integer.toHexString(base.hashCode());
    }

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
    public static String getParentCommit(String currentBranch) throws IOException {
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

    public static Map<String, String> readCommitFiles(String commitHash) throws IOException {
        Map<String, String> files = new HashMap<>();
        File commitFile = new File(".dotgit/objects/" + commitHash);

        if (!commitFile.exists()) {
            System.err.println("Commit " + commitHash + " does not exist.");
            return files;
        }

        List<String> lines = Files.readAllLines(commitFile.toPath(), StandardCharsets.UTF_8);

        for (String line : lines) {
            if (!line.startsWith("message:") && !line.startsWith("parent:")) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    files.put(parts[0], parts[1]); // filename -> hash
                }
            }
        }

        return files;
    }

    public static void commitWithParents(String message, String... parentHashes) throws IOException {
        File indexFile = new File(".dotgit/index");
        if (!indexFile.exists() || indexFile.length() == 0) {
            System.out.println("No changes to commit.");
            return;
        }

        Map<String, String> stagedFiles = new HashMap<>();
        List<String> indexEntries = Files.readAllLines(indexFile.toPath(), StandardCharsets.UTF_8);

        for (String entry : indexEntries) {
            String[] parts = entry.split(" ");
            if (parts.length == 2) {
                stagedFiles.put(parts[0], parts[1]);
            }
        }
        StringBuilder commitContent = new StringBuilder();
        commitContent.append("message: ").append(message).append("\n");
        if (parentHashes != null && parentHashes.length > 0) {
            for (String parentHash : parentHashes) {
                commitContent.append("parent: ").append(parentHash).append("\n");
            }
        }
        for (Map.Entry<String, String> entry : stagedFiles.entrySet()) {
            commitContent.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
        }
        String commitHash = Utils.computeHash(commitContent.toString());
        File commitFile = new File(".dotgit/objects/" + commitHash);
        Files.writeString(commitFile.toPath(), commitContent.toString());
        File headFile = new File(".dotgit/HEAD");
        String branchName = Utils.readFile(headFile).replace("ref: refs/heads/", "").trim();
        File branchFile = new File(".dotgit/refs/heads/" + branchName);
        Files.writeString(branchFile.toPath(), commitHash);

        Files.delete(indexFile.toPath());
        System.out.println("Committed with hash: " + commitHash);
    }

}
