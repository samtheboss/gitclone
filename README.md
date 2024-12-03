# Git Control System

A JavaFX-based Git system that simulates basic Git operations like **commit**, **merge**, **view commit history**, and **ignore files**. This project includes an intuitive GUI that allows users to interact with Git-like features, including the ability to execute commands such as `git commit`.

---

## Features

### Core Git Functionalities
- **Commit Changes**  
  Save the current state of tracked files with a descriptive message, automatically handling file hashes and metadata.

- **View Commit History**  
  Retrieve a list of all commits for the current branch, displaying commit messages, hashes, and parent relationships.

- **Merge Commits**  
  Merge changes between branches or commits, with conflict handling if necessary. Applies merged changes to the working directory.

- **Git Ignore**  
  Exclude specified files or directories from being tracked or committed.

- **Access Commit Files**  
  Retrieve files associated with any commit for easy exploration.

### GUI Features
- **Text Command Execution**  
  Type commands like `git commit` , `git add .` `git init` in a text field and execute them by pressing Enter.

- **Real-Time Feedback**  
  Displays success or error messages in dialog boxes or console output.

- **Interactive Commit Input**  
  Input commit messages via a text field for seamless interaction.

---

## Prerequisites

Before running the application, ensure you have the following set up:

1. **Java Development Kit (JDK)**  
   Version 11 or later.
   
2. **JavaFX SDK**  
   Download and configure JavaFX to enable GUI support.

3. **File Structure**  
   Ensure the `.dotgit` directory is created in your project root, with the following subdirectories:
 
 

.dotgit/
  ├── objects/          # Stores file contents hashed by SHA-256
  ├── refs/heads/       # Tracks branches and their latest commit hashes
  ├── commits/          # Metadata for each commit
.gitignore              # File to specify ignored paths


## How to Run

1. **Set Up the Project**
- Clone this repository.
- Open the project in your favorite IDE (e.g., IntelliJ IDEA, Eclipse, or VSCode).
- Ensure all dependencies are configured, including JavaFX.

2. **Run the Application**
- Execute the `GitCommitUI` class.

3. **Using Commands**
- **Commit Changes**: Type `git commit` in the text field.  
- **View History**: Type `git history` in the text field.  
- **Merge Branches**: Type `git merge <branch-name>` in the text field.

---

## File Structure


---

## Example Usage

### Commit Changes
1. Modify files in your working directory.
2. Enter `git commit -m 'message'` in the GUI text field.
3. Enter `git add .` in the GUI text field.

### View Commit History
1. click on the commit button then  click on view history
2. A list of commit messages with their hashes will be displayed.

### Merge Branches
1. Type `git merge '<branch-name>'` in the GUI text field.
2. If conflicts arise, they will be logged for resolution.

---

## Code Highlights

### Commit Logic
Handles creating a commit:
```java
 public static void commit(String message) {
        String author = "test"; // Author of the commit
        try {
            String branchName = GitServices.getCurrentBranch(); // Get the current branch name
            if (branchName.isEmpty()) { // Check if the branch name is empty
                System.out.println("No active branch found. Please create or switch to a branch.");
                return; // Exit if no active branch
            }}}
```
### merge Logic
Handles creating a merge:
```java
    public static void merge(String targetBranch, String author) {
        try {
            // Get the latest commit hashes for the branches
            String currentBranch = GitServices.getCurrentBranch();
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
```
### read commit logic
Handles read commit a files:
```java
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

```
###Create branch logic
Handles create branch:
```java
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
