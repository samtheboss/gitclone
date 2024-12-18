package com.smartapps.gitcontrolsystem.controllers;

import com.smartapps.gitcontrolsystem.service.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class UiController implements Initializable {
    @FXML
    private Button btnInit;

    @FXML
    private Button btnAdd;
    @FXML
    private Button btnMerger;

    @FXML
    private MenuButton mbCommits;

    @FXML
    private MenuItem mnCommit;

    @FXML
    private MenuItem mnCommitHistory;

    @FXML
    private MenuButton mbBranches;

    @FXML
    private MenuItem mnCreateBranch;

    @FXML
    private MenuItem mnGetCurrentBranch;

    @FXML
    private MenuItem mbSwitchBranch;
    @FXML
    private MenuItem mnListOfBranches;
    @FXML
    private MenuItem mnAllCommit;

    @FXML
    private Button btnGetLogs;

    @FXML
    private TreeView<?> TrVList;

    @FXML
    private TextArea tctCommands;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setListeners();
    }

    void setListeners() {
        btnInit.setOnAction(e -> {
            tctCommands.setText(GitServices.init());
        });
        btnAdd.setOnAction(e -> {
            openFileChooser();
        });
        btnGetLogs.setOnAction(e -> {
            GitServices.log();
        });
        mnGetCurrentBranch.setOnAction(e -> {
            tctCommands.setText(GitServices.getCurrentBranch());
        });
        mnCreateBranch.setOnAction(e -> {
            String value = showInputPopup();
            GitServices.createBranch(value);

        });
        mnCommit.setOnAction(e -> {
            String value = showInputPopup();
            GitCommit.commit(value);
        });
        mnCommitHistory.setOnAction(e -> {
            tctCommands.setText(GitCommit.viewCommitHistory());
            ;
        });
        mbSwitchBranch.setOnAction(e -> {
            String value = showInputPopup();
            GitServices.switchBranch(value);
        });
        mnListOfBranches.setOnAction(e -> {
            tctCommands.setText(GitServices.listBranches());
        });
        mnAllCommit.setOnAction(e -> {
            StringBuilder commitHash = new StringBuilder();
            List<String> listOfCommitHashMaps =
                    GitCommitFiles.getAllCommitHashes();
            assert listOfCommitHashMaps != null;
            for (String m : listOfCommitHashMaps) {
                commitHash.append(m).append("\n");
            }
            //tctCommands.setText(commitHash.toString());
        });
        btnMerger.setOnAction(e -> {
            String value = showInputPopup();
            try {
                MergeAndConflict.merge(value);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        tctCommands.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String inputText = tctCommands.getText().trim().replaceAll("\\s+", " ");

                if ("git init".equals(inputText)) {
                    // Initialize Git repository
                    tctCommands.setText(GitServices.init());
                } else if ("git add".equals(inputText)) {
                    // Open file chooser for adding files
                    openFileChooser();
                } else if (inputText.matches("git commit -m '.*'")) {
                    // Extract and handle the commit message
                    String commitMessage = inputText.replaceFirst("git commit -m '", "").replaceFirst("'$", "");
                    try {
                        GitCommit.commit(commitMessage);
                        tctCommands.setText("Committed: " + commitMessage);
                    } catch (Exception ex) {
                        System.err.println("Commit failed: " + ex.getMessage());
                        tctCommands.setText("Commit failed: " + ex.getMessage());
                    }
                } else {
                    // Handle unknown commands
                    tctCommands.setText("Unknown command: " + inputText);
                }
            }
        });

    }

    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Files", "*.java"));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(btnAdd.getScene().getWindow());
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            List<String> filesToAdd = new ArrayList<>();
            for (File file : selectedFiles) {
                filesToAdd.add(file.getAbsolutePath());
            }
            tctCommands.setText(GitServices.add(filesToAdd));
            showAlert(Alert.AlertType.INFORMATION, "Files Staged", "Successfully staged the selected files.");
        } else {
            showAlert(Alert.AlertType.WARNING, "No Files Selected", "No files were selected. Please try again.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String showInputPopup() {
        final String[] value = {""};
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(btnGetLogs.getScene().getWindow());
        popupStage.setTitle("Enter Value");
        TextField textField = new TextField();
        textField.setPromptText("Enter something...");
        Button okButton = new Button("OK");
        okButton.setOnAction(event -> {
            String inputValue = textField.getText();
            System.out.println("Input Value: " + inputValue);
            value[0] = inputValue;
            popupStage.close();
        });
        VBox popupLayout = new VBox();
        popupLayout.setSpacing(10);
        popupLayout.setPadding(new Insets(10));
        popupLayout.setAlignment(Pos.BOTTOM_RIGHT);
        popupLayout.getChildren().addAll(textField, okButton);
        popupStage.setScene(new Scene(popupLayout, 150, 100));
        popupStage.showAndWait();
        return value[0];
    }
}

