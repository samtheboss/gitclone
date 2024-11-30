package com.smartapps.gitcontrolsystem.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import static com.smartapps.gitcontrolsystem.service.GitServices.getCurrentBranch;

public class MergeAndConflict {

    public static void merge(String targetBranch) throws IOException {
        String currentBranch = getCurrentBranch();
        if (currentBranch.equals(targetBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        String currentHeadHash = getBranchHeadHash(currentBranch);
        String targetHeadHash = getBranchHeadHash(targetBranch);
        System.out.println(currentHeadHash+" "+targetHeadHash);
        if (currentHeadHash == null || targetHeadHash == null) {
            System.err.println("Error: Could not locate branch head hashes.");
            return;
        }

        System.out.println("Merging branch '" + targetBranch + "' into '" + currentBranch + "'.");

        Map<String, String> currentFiles = readCommitFiles(currentHeadHash);
        Map<String, String> targetFiles = readCommitFiles(targetHeadHash);

        // Debugging: print file maps
        System.out.println("Files in current branch: " + currentFiles);
        System.out.println("Files in target branch: " + targetFiles);

        Map<String, String> mergedFiles = resolveMerge(currentFiles, targetFiles);

        applyMergedChanges(mergedFiles);

        // Commit the merge with parent references
        String mergeMessage = "Merge branch '" + targetBranch + "' into '" + currentBranch + "'";
        GitCommit.commitWithParents(mergeMessage, currentHeadHash, targetHeadHash);

        System.out.println("Merge completed successfully.");
    }

    private static String getBranchHeadHash(String branchName) throws IOException {
        File branchFile = new File(".dotgit/refs/heads/" + branchName);
        if (!branchFile.exists()) {
            return null;
        }
        return Files.readString(branchFile.toPath()).trim();
    }

    private static Map<String, String> readCommitFiles(String commitHash) throws IOException {
        Map<String, String> files = new HashMap<>();
        File commitFile = new File(".dotgit/objects/" + commitHash);
        if (!commitFile.exists()) {
            System.err.println("Commit " + commitHash + " does not exist.");
            return files;
        }

        for (String line : Files.readAllLines(commitFile.toPath())) {
            if (!line.startsWith("message:") && !line.startsWith("parent:")) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    files.put(parts[0], parts[1]); // filename -> hash
                }
            }
        }
        return files;
    }

    private static Map<String, String> resolveMerge(Map<String, String> currentFiles, Map<String, String> targetFiles) {
        Map<String, String> mergedFiles = new HashMap<>(currentFiles);

        for (Map.Entry<String, String> entry : targetFiles.entrySet()) {
            String fileName = entry.getKey();
            String targetHash = entry.getValue();
            String currentHash = currentFiles.get(fileName);

            // Handle missing files or conflicts
            if (currentHash == null) {
                System.out.println("File " + fileName + " does not exist in the current branch. Adding it from the target branch.");
                mergedFiles.put(fileName, targetHash);
            } else if (!currentHash.equals(targetHash)) {
                System.err.println("Conflict detected for file: " + fileName);
                // Placeholder for conflict resolution logic
                mergedFiles.put(fileName, targetHash); // Take target branch's version for now
            }
        }

        return mergedFiles;
    }

    private static void applyMergedChanges(Map<String, String> mergedChanges) throws IOException {
        for (Map.Entry<String, String> entry : mergedChanges.entrySet()) {
            String fileName = entry.getKey();
            String fileHash = entry.getValue();

            File file = new File(fileName);

            // Check if the parent directory exists, and create it if necessary
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            } else {
                // Handle the case where the parent directory is null
                System.out.println("Parent directory for " + fileName + " is null or already exists.");
            }

            // Copy the file from object hash to the specified file
            File objectFile = new File(".dotgit/objects/" + fileHash);
            if (objectFile.exists()) {
                Files.copy(objectFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                System.out.println("Object file " + objectFile + " does not exist.");
            }
        }
    }

}
