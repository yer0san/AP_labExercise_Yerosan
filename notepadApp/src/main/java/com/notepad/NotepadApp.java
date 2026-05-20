package com.notepad;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.nio.file.Files;

public class NotepadApp extends Application {

    private TextArea textArea;
    private File currentFile;

    @Override
    public void start(Stage stage) {
        textArea = new TextArea();
        textArea.setWrapText(true);

        MenuBar menuBar = buildMenuBar(stage);

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(textArea);

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Notepad");
        stage.setScene(scene);
        stage.show();
    }

    private MenuBar buildMenuBar(Stage stage) {
        // File menu
        MenuItem newItem  = new MenuItem("New");
        MenuItem openItem = new MenuItem("Open...");
        MenuItem saveItem = new MenuItem("Save");
        MenuItem exitItem = new MenuItem("Exit");

        newItem.setOnAction(e  -> textArea.clear());
        openItem.setOnAction(e -> openFile(stage));
        saveItem.setOnAction(e -> saveFile(stage));
        exitItem.setOnAction(e -> stage.close());

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(newItem, openItem, saveItem,
                                   new SeparatorMenuItem(), exitItem);
        return new MenuBar(fileMenu);
    }

    private void openFile(Stage stage) {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            try {
                textArea.setText(Files.readString(file.toPath()));
                currentFile = file;
            } catch (IOException ex) { showError(ex); }
        }
    }

    private void saveFile(Stage stage) {
        if (currentFile == null) {
            FileChooser fc = new FileChooser();
            currentFile = fc.showSaveDialog(stage);
        }
        if (currentFile != null) {
            try (var w = new FileWriter(currentFile)) {
                w.write(textArea.getText());
            } catch (IOException ex) { showError(ex); }
        }
    }

    private void showError(Exception ex) {
        new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
    }
}