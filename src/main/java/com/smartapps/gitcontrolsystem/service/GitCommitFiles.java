package com.smartapps.gitcontrolsystem.service;

import com.smartapps.gitcontrolsystem.utils.Utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class GitCommitFiles {

    // Get the list of files committed in a specific commit
    public static Map<String, String> getCommittedFiles(String commitHash) throws IOException {
        Map<String, String> committedFiles = new HashMap<>();

        // Locate the commit directory
        File commitDirectory = new File(".dotgit/commits/" + commitHash);
        if (!commitDirectory.exists()) {
            System.out.println("Commit not found: " + commitHash);
            return committedFiles;
        }

        // Check if there are any files inside this commit folder
        File[] files = commitDirectory.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("No files found for commit: " + commitHash);
            return committedFiles;
        }

        // Read each file and get its content
        for (File file : files) {
            if (file.isFile()) {
                String content = new String(Files.readAllBytes(file.toPath()));
                committedFiles.put(file.getName(), content);
            }
        }

        return committedFiles;
    }
    public static List<String> getAllCommitHashes()  {
        try {
        List<String> commitHashes = new ArrayList<>();
        String currentBranch = GitServices.getCurrentBranch();
        String currentCommitHash = Utils.getBranchCommit(currentBranch); // or from your own getBranchCommit() method

        while (currentCommitHash != null) {
            commitHashes.add(currentCommitHash);
            currentCommitHash = GitCommit.getParentCommit(currentCommitHash); // Retrieve the parent commit
        }

        Collections.reverse(commitHashes); // Reverse to get from oldest to latest commit
        return commitHashes;}catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }
}
