package com.smartapps.gitcontrolsystem.service;

import com.smartapps.gitcontrolsystem.utils.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class GitServices {
    public static void main(String[] args) {
        // init();
        createBranch("testbranch");
    }

    public static void init() {
        try {
            File dotgit = new File(".dotgit");
            if (!dotgit.exists()) {
                // Create directory structure
                new File(".dotgit/hooks").mkdirs();
                new File(".dotgit/commits").mkdirs();
                new File(".dotgit/info").mkdirs();
                new File(".dotgit/objects").mkdirs();
                new File(".dotgit/refs/heads").mkdirs();
                new File(".dotgit/refs/tags").mkdirs();
                // Initialize HEAD file
                File head = new File(".dotgit/HEAD");
                head.createNewFile();
                Utils.writeToFile(head, "ref: refs/heads/main");
                System.out.println("Initialized empty Git repository in .dotgit/");
            } else {
                System.out.println("Repository already exists!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void add(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File " + fileName + " does not exist!");
            return;
        }
        // Read file content and compute a hash for tracking
        String fileContent = new String(Files.readAllBytes(Paths.get(fileName)));
        String fileHash = Utils.computeHash(fileContent);
        // Save the file content in the `.dotgit/objects` directory
        File objectFile = new File(".dotgit/objects/" + fileHash);
        if (!objectFile.exists()) {
            objectFile.getParentFile().mkdirs();
            objectFile.createNewFile();
            Utils.writeToFile(objectFile, fileContent);
        }   // Add the file to the staging area (index)
        File index = new File(".dotgit/index");
        String entry = fileName + " " + fileHash + "\n";
       Utils.appendToFile(index, entry);

        System.out.println("File " + fileName + " added to staging area.");
    }
    public static void commit(String message) throws IOException {
        // Read staged files from the index
        File index = new File(".dotgit/index");
        if (!index.exists() || index.length() == 0) {
            System.out.println("Nothing to commit!");
            return;
        }
        String stagedFiles = new String(Files.readAllBytes(index.toPath()));
        // Read current HEAD
        File head = new File(".dotgit/HEAD");
        String headContent = Utils.readFile(head).trim();
        String parentCommit = "";
        if (headContent.startsWith("ref: ")) {
            String branchName = headContent.substring(5).trim();
            File branchFile = new File(".dotgit/" + branchName);
            if (branchFile.exists()) {
                parentCommit = Utils.readFile(branchFile).trim();
            }
        } else {
            parentCommit = headContent;
        }
        // Generate commit hash
        String commitHash = Utils.computeCommitHash(stagedFiles, parentCommit, message);
        // Write commit file
        File commitFile = new File(".dotgit/commits/" + commitHash);
        commitFile.createNewFile();
        String commitContent = String.format("""
                parent: %s
                timestamp: %s
                message: %s
                files:
                %s
                """, parentCommit, Instant.now().toString(), message, stagedFiles);
        Utils.writeToFile(commitFile, commitContent);
        // Update the current branch (or detached HEAD if no branch)
        if (headContent.startsWith("ref: ")) {
            String branchName = headContent.substring(5).trim();
            File branchFile = new File(".dotgit/" + branchName);
            Utils.writeToFile(branchFile, commitHash);
        } else {
            Utils.writeToFile(head, commitHash);
        }
        // Clear the index
        new PrintWriter(index).close();

        System.out.println("Committed with hash: " + commitHash);
    }

    public static void createBranch(String branchName) {
        // Get the current HEAD commit hash
        File head = new File(".dotgit/HEAD");
        File masters = new File(".dotgit/refs/heads/master");
        if (!masters.exists()) {
            System.out.println("not a valid object name: 'master'");
        }
        try {
            String currentCommit = Utils.readFile(head).split(": ")[1].trim();
            // Create a new branch file
            File branchFile = new File(".dotgit/refs/heads/" + branchName);
            if (branchFile.exists()) {
                System.out.println("Branch " + branchName + " already exists!");
                return;
            }
            branchFile.createNewFile();
            Utils.writeToFile(branchFile, currentCommit);
            System.out.println("Branch " + branchName + " created at commit " + currentCommit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void log() {
        // Start with the current HEAD
        try {
            File head = new File(".dotgit/HEAD");
            String currentCommit = Utils.readFile(head).split(": ")[1].trim();
            while (!currentCommit.isEmpty()) {
                File commitFile = new File(".dotgit/commits/" + currentCommit);
                if (!commitFile.exists()) {
                    break;
                }
                String commitContent = Utils.readFile(commitFile);
                System.out.println(commitContent);
                currentCommit = commitContent.lines()
                        .filter(line -> line.startsWith("parent: "))
                        .map(line -> line.split(": ")[1].trim())
                        .findFirst().orElse("");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void switchBranch(String branchName) throws IOException {
        File branchFile = new File(".dotgit/refs/heads/" + branchName);
        if (!branchFile.exists()) {
            System.out.println("Branch " + branchName + " does not exist!");
            return;
        }
        // Update HEAD to point to the branch
        File head = new File(".dotgit/HEAD");
        Utils.writeToFile(head, "ref: refs/heads/" + branchName);
        System.out.println("Switched to branch " + branchName);
    }

    private static String getCurrentBranch() throws IOException {
        File head = new File(".dotgit/HEAD");
        String headContent = Utils.readFile(head).trim();
        if (headContent.startsWith("ref: ")) {
            return headContent.substring(5).trim().replace("refs/heads/", "");
        }
        return "detached HEAD";
    }
    public static void listStagedFiles() throws IOException {
        File indexFile = new File(".dotgit/index");
        if (!indexFile.exists() || indexFile.length() == 0) {
            System.out.println("No files are staged.");
            return;
        }
        BufferedReader reader = new BufferedReader(new FileReader(indexFile));
        String line;
        System.out.println("Staged files:");
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");
            String fileName = parts[0];
            System.out.println("- " + fileName);
        }
        reader.close();
    }

}
