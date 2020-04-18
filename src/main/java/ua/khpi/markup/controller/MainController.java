package ua.khpi.markup.controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.DirectoryChooser;
import ua.khpi.markup.exception.FileRenamingException;
import ua.khpi.markup.exception.FileSavingException;
import ua.khpi.markup.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static ua.khpi.markup.util.ControllerUtils.showAlert;

public class MainController {

    private static final String EASY_MARKUP_PREFIX = "<easy>";
    private static final String MEDIUM_MARKUP_PREFIX = "<medium>";
    private static final String COMPLEX_MARKUP_PREFIX = "<complex>";
    private static final String EASY_MARKUP_SUFFIX = "</easy>";
    private static final String MEDIUM_MARKUP_SUFFIX = "</medium>";
    private static final String COMPLEX_MARKUP_SUFFIX = "</complex>";
    private static final int EASY = 1;
    private static final int MEDIUM = 2;
    private static final int COMPLEX = 3;

    private Iterator<Path> iterator;
    private Path currentFile;

    @FXML
    private TextField fileChooserText;
    @FXML
    private TextArea textArea;
    @FXML
    private Label currentTextLabel;
    @FXML
    private Button skipBtn;
    @FXML
    private Button btnEasy;
    @FXML
    private Button btnMedium;
    @FXML
    private Button btnComplex;

    @FXML
    void choosePathToTexts(ActionEvent event) {
        processingFinished();

        File directoryWithTexts = getDirectoryWithTextsToProcess(event);
        if (directoryWithTexts == null) return;

        try {
            initializeProcessing(directoryWithTexts);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "IOException",
                    "An error occurred while trying to get texts from chosen directory");
        }
    }

    @FXML
    void skipText(ActionEvent event) {
        Optional<ButtonType> wannaSkip = showAlert(Alert.AlertType.CONFIRMATION, "Are you sure?",
                "This file will be skipped but will not be marked as processed");
        if ((wannaSkip.isPresent()) && (wannaSkip.get() == ButtonType.OK)) {
            processNextFile();
        }
    }

    @FXML
    void markAsEasy(ActionEvent event) {
        processTextWithComplexity(EASY);
    }

    @FXML
    void markAsMedium(ActionEvent event) {
        processTextWithComplexity(MEDIUM);
    }

    @FXML
    void markAsComplex(ActionEvent event) {
        processTextWithComplexity(COMPLEX);
    }

    private void processTextWithComplexity(int complexity) {
        markupText(complexity);
        saveText();
        processNextFile();
    }

    private void markupText(int complexity) {
        switch (complexity) {
            case EASY:
                textArea.setText(EASY_MARKUP_PREFIX + textArea.getText() + EASY_MARKUP_SUFFIX);
                break;

            case MEDIUM:
                textArea.setText(MEDIUM_MARKUP_PREFIX + textArea.getText() + MEDIUM_MARKUP_SUFFIX);
                break;

            case COMPLEX:
                textArea.setText(COMPLEX_MARKUP_PREFIX + textArea.getText() + COMPLEX_MARKUP_SUFFIX);
                break;
        }
    }

    private void saveText() {
        try {
            FileUtils.saveProcessedFile(currentFile, textArea.getText());
        } catch (FileSavingException e) {
            showAlert(Alert.AlertType.ERROR, "Unable to save file",
                    "Unable to save file, skipping it for now");
        } catch (FileRenamingException e) {
            showAlert(Alert.AlertType.ERROR, "Unable to mark file as processed",
                    "Your markup was saved but file was not marked as processed");
        }
    }

    public void init() {
        textArea.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, Event::consume);
        textArea.setEditable(false);
        textArea.setStyle("-fx-opacity: 1;");
    }

    private File getDirectoryWithTextsToProcess(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose a folder");
        return dirChooser.showDialog(((Node) event.getSource()).getScene().getWindow());
    }

    private void initializeProcessing(File directoryWithTexts) throws IOException {
        List<Path> filesToProcess = FileUtils.getFilesToProcess(directoryWithTexts);

        if (filesToProcess.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No files found",
                    "Found 0 files with .txt extension that was not processed");
            return;
        }

        Optional<ButtonType> confirmProcessing = showAlert(Alert.AlertType.CONFIRMATION, "Files was found",
                String.format("Found %s files to process, proceed?", filesToProcess.size()));

        if (confirmProcessing.isPresent() && confirmProcessing.get() == ButtonType.OK) {
            fileChooserText.setText(directoryWithTexts.getAbsolutePath());
            this.iterator = filesToProcess.iterator();
            setTextProcessingButtonsEnabled(true);
            processNextFile();
        }
    }

    private void processNextFile() {
        if (!iterator.hasNext()) {
            processingFinished();
            return;
        }

        currentFile = iterator.next();
        currentTextLabel.setText(String.format("Processing %s", currentFile.getFileName()));
        try {
            String fileContent = FileUtils.getFileContent(currentFile);
            textArea.setText(fileContent);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Can not open file",
                    String.format("Exception occurred while opening %s%nTrying to open next file in list",
                            currentFile.getFileName()));
            processNextFile();
        }
    }

    private void processingFinished() {
        currentTextLabel.setText("Processing finished");
        textArea.clear();
        setTextProcessingButtonsEnabled(false);
    }

    private void setTextProcessingButtonsEnabled(boolean enabled) {
        skipBtn.setDisable(!enabled);
        btnEasy.setDisable(!enabled);
        btnMedium.setDisable(!enabled);
        btnComplex.setDisable(!enabled);
    }
}

