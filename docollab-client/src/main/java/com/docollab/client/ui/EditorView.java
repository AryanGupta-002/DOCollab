package com.docollab.client.ui;

import com.docollab.client.storage.SQLiteManager;
import com.docollab.client.network.SyncManager;
import com.docollab.shared.algorithm.CRDTNode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextArea;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EditorView extends Application {

    private SQLiteManager dbManager;
    private SyncManager syncManager;
    private InlineCssTextArea textArea;
    private Scene scene;

    private final List<CRDTNode> crdtDocument = new ArrayList<>();
    private boolean isApplyingRemoteChange = false;

    private String currentUser;
    private String currentServerUrl;
    private boolean isDarkMode = false;

    private ToggleButton boldBtn, italicBtn, underlineBtn;
    private ComboBox<String> fontCombo;
    private ComboBox<Integer> sizeCombo;
    private ColorPicker colorPicker;
    private ToggleButton alignLeftBtn, alignCenterBtn, alignRightBtn;

    private Button userBtn;

    @Override
    public void init() {
        dbManager = new SQLiteManager();
    }

    @Override
    public void start(Stage primaryStage) {
        promptForConnection();

        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());

        textArea = new InlineCssTextArea();
        textArea.setWrapText(true);
        textArea.setPadding(new Insets(80));
        textArea.setMaxWidth(816);
        textArea.setMinHeight(1056);
        textArea.getStyleClass().add("paper");

        StackPane desk = new StackPane(textArea);
        desk.getStyleClass().add("desk");
        desk.setPadding(new Insets(40));

        ScrollPane scrollPane = new ScrollPane(desk);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("desk-scroll");

        root.setCenter(scrollPane);

        syncManager = new SyncManager(remoteNode -> {
            Platform.runLater(() -> {
                isApplyingRemoteChange = true;

                if (remoteNode.isDeleted()) {
                    int actualIndex = findActualIndexById(remoteNode.getId());
                    if (actualIndex != -1) {
                        crdtDocument.get(actualIndex).setDeleted(true);
                        int visibleIndex = calculateVisibleIndex(actualIndex);
                        textArea.deleteText(visibleIndex, visibleIndex + 1);
                    }
                } else {
                    int insertIndex = 0;
                    if (!remoteNode.getLeftId().equals("START")) {
                        int leftNodeIndex = findActualIndexById(remoteNode.getLeftId());
                        insertIndex = leftNodeIndex != -1 ? leftNodeIndex + 1 : crdtDocument.size();
                    }

                    crdtDocument.add(insertIndex, remoteNode);
                    int visibleIndex = calculateVisibleIndex(insertIndex);

                    textArea.insertText(visibleIndex, String.valueOf(remoteNode.getValue()));

                    StringBuilder charStyle = new StringBuilder();
                    if (remoteNode.isBold()) charStyle.append("-fx-font-weight: bold; ");
                    if (remoteNode.isItalic()) charStyle.append("-fx-font-style: italic; ");
                    if (remoteNode.isUnderline()) charStyle.append("-fx-underline: true; ");
                    charStyle.append("-fx-font-family: '").append(remoteNode.getFontFamily()).append("'; ");
                    charStyle.append("-fx-font-size: ").append(remoteNode.getFontSize()).append("px; ");
                    charStyle.append("-fx-fill: ").append(remoteNode.getColorHex()).append("; ");

                    String authorName = remoteNode.getId().split("-")[0];
                    charStyle.append("-rtfx-background-color: ").append(getUserHighlightColor(authorName)).append("; ");

                    textArea.setStyle(visibleIndex, visibleIndex + 1, charStyle.toString());

                    int pIndex = 0;
                    for (int i = 0; i < visibleIndex; i++) {
                        if (textArea.getText().charAt(i) == '\n') pIndex++;
                    }
                    textArea.setParagraphStyle(pIndex, "-fx-text-alignment: " + remoteNode.getAlignment() + "; ");
                }

                dbManager.saveNodeLocally(remoteNode, true);
                isApplyingRemoteChange = false;
            });
        });

        syncManager.connectToServer(currentUser, currentServerUrl);

        textArea.plainTextChanges().subscribe(change -> {
            if (isApplyingRemoteChange) return;

            String inserted = change.getInserted();
            String removed = change.getRemoved();
            int visiblePosition = change.getPosition();

            if (!removed.isEmpty()) {
                for (int i = 0; i < removed.length(); i++) {
                    int actualPos = getActualIndexFromVisible(visiblePosition);
                    if (actualPos < crdtDocument.size()) {
                        CRDTNode deletedNode = crdtDocument.get(actualPos);
                        deletedNode.setDeleted(true);
                        dbManager.saveNodeLocally(deletedNode, false);
                        syncManager.broadcastNode(deletedNode);
                    }
                }
            }

            if (!inserted.isEmpty()) {
                for (int i = 0; i < inserted.length(); i++) {
                    char typedChar = inserted.charAt(i);
                    String uniqueId = currentUser + "-" + System.currentTimeMillis() + "-" + i;

                    int currentVisiblePos = visiblePosition + i;
                    int actualPos = getActualIndexFromVisible(currentVisiblePos);
                    String leftId = actualPos > 0 ? crdtDocument.get(actualPos - 1).getId() : "START";

                    CRDTNode node = new CRDTNode(uniqueId, typedChar, leftId);

                    node.setBold(boldBtn.isSelected());
                    node.setItalic(italicBtn.isSelected());
                    node.setUnderline(underlineBtn.isSelected());
                    node.setFontFamily(fontCombo.getValue());
                    node.setFontSize(sizeCombo.getValue());
                    node.setColorHex(toHexString(colorPicker.getValue()));
                    if (alignCenterBtn.isSelected()) node.setAlignment("center");
                    else if (alignRightBtn.isSelected()) node.setAlignment("right");
                    else node.setAlignment("left");

                    crdtDocument.add(actualPos, node);
                    dbManager.saveNodeLocally(node, false);
                    syncManager.broadcastNode(node);
                }
                Platform.runLater(this::applyCurrentStyleToEditor);
            }
        });

        scene = new Scene(root, 1100, 750);
        applyTheme();
        primaryStage.setTitle("DOCollab - Document Editor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void promptForConnection() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Welcome to DOCollab!");
        dialog.setHeaderText("Welcome Aboard , Set Your DOCollab Identity-");

        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20, 20, 10, 10));

        TextField usernameInput = new TextField(currentUser != null ? currentUser : "");
        usernameInput.setPromptText("Enter display name...");

        TextField urlInput = new TextField(currentServerUrl != null ? currentServerUrl : "");
        urlInput.setPromptText("e.g., wss://your-ngrok-link.ngrok.io/ws/editor");
        urlInput.setPrefWidth(350);

        CheckBox offlineCheck = new CheckBox("Use Offline (Localhost)");
        offlineCheck.setOnAction(e -> {
            if (offlineCheck.isSelected()) {
                urlInput.setText("ws://localhost:8080/ws/editor");
                urlInput.setDisable(true);
            } else {
                urlInput.clear();
                urlInput.setDisable(false);
            }
        });

        vbox.getChildren().addAll(
                new Label("Please enter your display name:"),
                usernameInput,
                new Label("Please enter your collaborative link:"),
                urlInput,
                offlineCheck
        );

        dialog.getDialogPane().setContent(vbox);
        Platform.runLater(usernameInput::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == connectButtonType) {
            String typedName = usernameInput.getText().trim().replaceAll("\\s+", "");
            currentUser = typedName.isEmpty() ? "Anonymous-" + (int)(Math.random() * 1000) : typedName;
            currentServerUrl = urlInput.getText().trim();

            if (userBtn != null) userBtn.setText(currentUser);
        } else if (currentUser == null) {
            currentUser = "OfflineUser-" + (int)(Math.random() * 1000);
            currentServerUrl = "";
        }
    }

    private int getActualIndexFromVisible(int visibleIndex) {
        int visibleCount = 0;
        for (int i = 0; i < crdtDocument.size(); i++) {
            if (!crdtDocument.get(i).isDeleted()) {
                if (visibleCount == visibleIndex) return i;
                visibleCount++;
            }
        }
        return crdtDocument.size();
    }

    private int findActualIndexById(String id) {
        for (int i = 0; i < crdtDocument.size(); i++) {
            if (crdtDocument.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    private int calculateVisibleIndex(int actualIndex) {
        int visibleCount = 0;
        for (int i = 0; i < actualIndex; i++) {
            if (!crdtDocument.get(i).isDeleted()) {
                visibleCount++;
            }
        }
        return visibleCount;
    }

    private void exportToFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Document as Text");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Document (*.txt)", "*.txt"));
        File file = fileChooser.showSaveDialog(scene.getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.print(textArea.getText());
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Your document has been safely exported!");
                alert.showAndWait();
            } catch (IOException ex) {
                System.err.println("ERROR: Could not save the file. " + ex.getMessage());
            }
        }
    }

    private String getUserHighlightColor(String username) {
        String[] pastelColors = { "#D6EAF8", "#E8DAEF", "#FDEBD0", "#FADBD8", "#FCF3CF", "#EBF5FB" };
        int hash = Math.abs(username.hashCode());
        return pastelColors[hash % pastelColors.length];
    }

    private VBox buildToolbar() {
        ToolBar toolBar = new ToolBar();

        boldBtn = new ToggleButton("B");
        boldBtn.setStyle("-fx-font-weight: bold;");
        italicBtn = new ToggleButton("I");
        italicBtn.setStyle("-fx-font-style: italic;");
        underlineBtn = new ToggleButton("U");
        underlineBtn.setStyle("-fx-underline: true;");

        fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll(javafx.scene.text.Font.getFamilies());
        fontCombo.setValue("Arial");

        sizeCombo = new ComboBox<>();
        sizeCombo.getItems().addAll(8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 36, 48, 72);
        sizeCombo.setValue(16);

        colorPicker = new ColorPicker(Color.BLACK);

        ToggleGroup alignGroup = new ToggleGroup();
        alignLeftBtn = new ToggleButton("Left");
        alignCenterBtn = new ToggleButton("Center");
        alignRightBtn = new ToggleButton("Right");
        alignLeftBtn.setToggleGroup(alignGroup);
        alignCenterBtn.setToggleGroup(alignGroup);
        alignRightBtn.setToggleGroup(alignGroup);
        alignLeftBtn.setSelected(true);

        Button themeBtn = new Button("Toggle Dark Mode");
        themeBtn.setOnAction(e -> {
            isDarkMode = !isDarkMode;
            applyTheme();
        });

        Button exportBtn = new Button("Export File");
        exportBtn.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E86C1;");
        exportBtn.setOnAction(e -> exportToFile());

        // NEW: Combined interactive User Badge
        userBtn = new Button("👤 " + currentUser);
        userBtn.setStyle("-fx-font-weight: bold; -fx-background-color: transparent; -fx-border-color: #cccccc; -fx-border-radius: 5; -fx-padding: 4 10 4 10;");
        userBtn.setOnAction(e -> {
            if (syncManager != null) syncManager.disconnect();
            promptForConnection();
            syncManager.connectToServer(currentUser, currentServerUrl);
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolBar.getItems().addAll(
                fontCombo, sizeCombo, new Separator(),
                boldBtn, italicBtn, underlineBtn, new Separator(),
                alignLeftBtn, alignCenterBtn, alignRightBtn, new Separator(),
                colorPicker, spacer, exportBtn, new Separator(), themeBtn, userBtn
        );

        return new VBox(toolBar);
    }

    private void applyCurrentStyleToEditor() {
        int currentPosition = textArea.getCaretPosition();
        if (currentPosition == 0) return;

        StringBuilder charStyle = new StringBuilder();
        if (boldBtn.isSelected()) charStyle.append("-fx-font-weight: bold; ");
        if (italicBtn.isSelected()) charStyle.append("-fx-font-style: italic; ");
        if (underlineBtn.isSelected()) charStyle.append("-fx-underline: true; ");

        charStyle.append("-fx-font-family: '").append(fontCombo.getValue()).append("'; ");
        charStyle.append("-fx-font-size: ").append(sizeCombo.getValue()).append("px; ");
        charStyle.append("-fx-fill: ").append(toHexString(colorPicker.getValue())).append("; ");

        charStyle.append("-rtfx-background-color: ").append(getUserHighlightColor(currentUser)).append("; ");

        textArea.setStyle(currentPosition - 1, currentPosition, charStyle.toString());

        int currentParagraph = textArea.getCurrentParagraph();
        if (alignCenterBtn.isSelected()) textArea.setParagraphStyle(currentParagraph, "-fx-text-alignment: center; ");
        else if (alignRightBtn.isSelected()) textArea.setParagraphStyle(currentParagraph, "-fx-text-alignment: right; ");
        else textArea.setParagraphStyle(currentParagraph, "-fx-text-alignment: left; ");
    }

    private void applyTheme() {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String cssFile = isDarkMode ? "/dark-theme.css" : "/light-theme.css";

        if (isDarkMode && colorPicker.getValue().equals(Color.BLACK)) colorPicker.setValue(Color.WHITE);
        else if (!isDarkMode && colorPicker.getValue().equals(Color.WHITE)) colorPicker.setValue(Color.BLACK);

        scene.getStylesheets().add(getClass().getResource(cssFile).toExternalForm());
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
    }
}