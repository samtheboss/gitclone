package com.smartapps.gitcontrolsystem.service;
import com.smartapps.gitcontrolsystem.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MergeAndConflict {
    public static void merge(String branchName) throws IOException {
        // Get the current branch and target branch commits
        File head = new File(".dotgit/HEAD");
        String currentBranch = Utils.readFile(head).split(": ")[1].trim();
        File branchFile = new File(".dotgit/refs/heads/" + branchName);
        if (!branchFile.exists()) {
            System.out.println("Branch " + branchName + " does not exist!");
            return;
        }
        String targetCommit = Utils.readFile(branchFile).trim();
        String currentCommit = Utils.readFile(new File(".dotgit/refs/heads/" + currentBranch)).trim();
        // Perform a basic three-way merge
        Map<String, String> currentFiles = getFilesFromCommit(currentCommit);
        Map<String, String> targetFiles = getFilesFromCommit(targetCommit);
        // Check for conflicts
        boolean conflictFound = false;
        for (String file : targetFiles.keySet()) {
            if (currentFiles.containsKey(file) && !currentFiles.get(file).equals(targetFiles.get(file))) {
                System.out.println("Conflict detected in file: " + file);
                conflictFound = true;
            }
        }

        if (conflictFound) {
            System.out.println("Merge aborted due to conflicts.");
            return;
        }

        // Merge changes
        currentFiles.putAll(targetFiles);
        commitMergedChanges(currentFiles, "Merged branch " + branchName);

        System.out.println("Branch " + branchName + " successfully merged.");
    }

    private static Map<String, String> getFilesFromCommit(String commitHash) throws IOException {
        File commitFile = new File(".dotgit/commits/" + commitHash);
        Map<String, String> files = new HashMap<>();
        if (!commitFile.exists()) {
            return files;
        }
        for (String line : Utils.readFile(commitFile).split("\n")) {
            if (line.startsWith("files:")) {
                break;
            }
        }

        return files;
    }
    private static void commitMergedChanges(Map<String, String> files, String message) throws IOException {
        File index = new File(".dotgit/index");
        try (PrintWriter writer = new PrintWriter(index)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                writer.println(entry.getKey() + " " + entry.getValue());
            }
        }
        GitServices.commit(message);
    }
}
