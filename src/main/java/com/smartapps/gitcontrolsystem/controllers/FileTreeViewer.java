package com.smartapps.gitcontrolsystem.controllers;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.File;

public class FileTreeViewer extends Application {

    @Override
    public void start(Stage primaryStage) {
        File rootDirectory = new File(System.getProperty("user.dir"));
        TreeItem<String> rootItem = new TreeItem<>(rootDirectory.getName());
        rootItem.setExpanded(true);
        buildFileTree(rootItem, rootDirectory);
        TreeView<String> treeView = new TreeView<>(rootItem);
        StackPane root = new StackPane();
        root.getChildren().add(treeView);
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("File Tree Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    private void buildFileTree(TreeItem<String> parentItem, File directory) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                TreeItem<String> childItem = new TreeItem<>(file.getName());
                parentItem.getChildren().add(childItem);
                if (file.isDirectory()) {
                    buildFileTree(childItem, file);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
