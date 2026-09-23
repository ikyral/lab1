package app;

import agent.Agent;
import agent.Herbivore;
import agent.Plant;
import agent.Predator;
import environment.Environment;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainApp extends Application {

    private Environment environment;
    private int turnCount = 0;
    private boolean isGameOver = false;
    private int stepMultiplier = 1;

    private Canvas canvas;
    private Pane canvasContainer;

    private Label stepLabel;
    private Label plantsLabel;
    private Label herbivoresLabel;
    private Label predatorsLabel;
    private Button stepButton;
    private Button autoButton;
    private Timeline autoTimeline;

    private Image plantImage;
    private Image herbivoreImage;
    private Image predatorImage;

    private static final Color COLOR_PLANT_BG = Color.web("#81c784");
    private static final Color COLOR_PLANT_BORDER = Color.web("#4caf50");
    private static final Color COLOR_HERBIVORE_BG = Color.web("#64b5f6");
    private static final Color COLOR_HERBIVORE_BORDER = Color.web("#1e88e5");
    private static final Color COLOR_PREDATOR_BG = Color.web("#e57373");
    private static final Color COLOR_PREDATOR_BORDER = Color.web("#e53935");
    private static final Color COLOR_EMPTY_BG = Color.web("#ffffff");
    private static final Color COLOR_GRID = Color.web("#e0e0e0");

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);

        int[] config = askGameParameters();
        if (config == null) {
            Platform.exit();
            System.exit(0);
            return;
        }

        Platform.setImplicitExit(true);

        int width = config[0];
        int height = config[1];
        int plants = config[2];
        int herbivores = config[3];
        int predators = config[4];

        environment = new Environment(width, height);
        environment.initGame(plants, herbivores, predators);
        initEmojiTextures();

        BorderPane root = new BorderPane();

        autoTimeline = new Timeline(new KeyFrame(Duration.millis(120), e -> doStep()));
        autoTimeline.setCycleCount(Timeline.INDEFINITE);

        VBox topPanel = createTopPanel();
        root.setTop(topPanel);

        canvasContainer = new Pane();
        canvasContainer.setStyle("-fx-background-color: #dcdcdc;");

        canvas = new Canvas();
        canvasContainer.getChildren().add(canvas);
        root.setCenter(canvasContainer);

        canvasContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            drawField();
        });
        canvasContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            drawField();
        });

        Scene scene = new Scene(root, 760, 720);
        primaryStage.setTitle("Искусственная жизнь");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(480);

        primaryStage.setOnCloseRequest(e -> {
            if (autoTimeline != null) {
                autoTimeline.stop();
            }
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();

        Platform.runLater(() -> {
            canvas.setWidth(canvasContainer.getWidth());
            canvas.setHeight(canvasContainer.getHeight());
            updateStats();
            drawField();
        });
    }

    private int[] askGameParameters() {
        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("Параметры симуляции");
        dialog.setHeaderText("Задайте размеры поля и начальные популяции:");
        dialog.setGraphic(null);

        Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
        dialogStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });

        ButtonType defaultButtonType = new ButtonType("Вернуть параметры по умолчанию", ButtonBar.ButtonData.LEFT);
        ButtonType okButtonType = new ButtonType("Запустить", ButtonBar.ButtonData.OK_DONE);
        ButtonType hiddenCancelType = new ButtonType("", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(defaultButtonType, okButtonType, hiddenCancelType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15, 20, 15, 20));

        Spinner<Integer> widthSpinner = new Spinner<>(1, 100, 50);
        widthSpinner.setEditable(true);

        Spinner<Integer> heightSpinner = new Spinner<>(1, 100, 50);
        heightSpinner.setEditable(true);

        Spinner<Integer> plantsSpinner = new Spinner<>(0, 10000, 900);
        plantsSpinner.setEditable(true);

        Spinner<Integer> herbivoresSpinner = new Spinner<>(0, 10000, 180);
        herbivoresSpinner.setEditable(true);

        Spinner<Integer> predatorsSpinner = new Spinner<>(0, 10000, 70);
        predatorsSpinner.setEditable(true);

        Label hintLabel = new Label("Параметры по умолчанию гарантируют стабильную симуляцию более, чем на 1000 ходов");
        hintLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #555555;");
        hintLabel.setWrapText(true);
        hintLabel.setMaxWidth(350);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(350);

        grid.add(new Label("Ширина поля [1; 100]:"), 0, 0);
        grid.add(widthSpinner, 1, 0);
        grid.add(new Label("Высота поля [1; 100]:"), 0, 1);
        grid.add(heightSpinner, 1, 1);
        grid.add(new Label("🌿 Растения:"), 0, 2);
        grid.add(plantsSpinner, 1, 2);
        grid.add(new Label("🐰 Травоядные:"), 0, 3);
        grid.add(herbivoresSpinner, 1, 3);
        grid.add(new Label("🐺 Хищники:"), 0, 4);
        grid.add(predatorsSpinner, 1, 4);
        grid.add(hintLabel, 0, 5, 2, 1);
        grid.add(statusLabel, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Runnable validate = () -> {
            int w = getSpinnerValue(widthSpinner, 50);
            int h = getSpinnerValue(heightSpinner, 50);
            int p = getSpinnerValue(plantsSpinner, 900);
            int herb = getSpinnerValue(herbivoresSpinner, 180);
            int pred = getSpinnerValue(predatorsSpinner, 70);

            long totalCells = (long) w * h;
            long totalAgents = (long) p + herb + pred;

            boolean dimsValid = w >= 1 && w <= 100 && h >= 1 && h <= 100;
            boolean agentsNonNegative = p >= 0 && herb >= 0 && pred >= 0;
            boolean countValid = totalAgents <= totalCells;
            boolean allValid = dimsValid && agentsNonNegative && countValid;

            if (!dimsValid) {
                statusLabel.setText("Размеры поля должны быть в диапазоне [1; 100].");
                statusLabel.setStyle("-fx-text-fill: #d32f2f;");
            } else if (!agentsNonNegative) {
                statusLabel.setText("Численность популяций не может быть отрицательной.");
                statusLabel.setStyle("-fx-text-fill: #d32f2f;");
            } else if (!countValid) {
                statusLabel.setText("Агентов (" + totalAgents + ") больше, чем клеток (" + totalCells + ").");
                statusLabel.setStyle("-fx-text-fill: #d32f2f;");
            } else {
                statusLabel.setText("Всего агентов: " + totalAgents + " / " + totalCells + " клеток");
                statusLabel.setStyle("-fx-text-fill: #2e7d32;");
            }

            Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
            if (okButton != null) {
                okButton.setDisable(!allValid);
            }
        };

        Node defaultButton = dialog.getDialogPane().lookupButton(defaultButtonType);
        if (defaultButton != null) {
            defaultButton.addEventFilter(ActionEvent.ACTION, event -> {
                widthSpinner.getValueFactory().setValue(50);
                widthSpinner.getEditor().setText("50");
                heightSpinner.getValueFactory().setValue(50);
                heightSpinner.getEditor().setText("50");
                plantsSpinner.getValueFactory().setValue(900);
                plantsSpinner.getEditor().setText("900");
                herbivoresSpinner.getValueFactory().setValue(180);
                herbivoresSpinner.getEditor().setText("180");
                predatorsSpinner.getValueFactory().setValue(70);
                predatorsSpinner.getEditor().setText("70");
                validate.run();
                event.consume();
            });
        }

        List<Spinner<Integer>> spinners = List.of(widthSpinner, heightSpinner, plantsSpinner, herbivoresSpinner, predatorsSpinner);
        for (Spinner<Integer> sp : spinners) {
            sp.valueProperty().addListener((obs, oldVal, newVal) -> validate.run());
            sp.getEditor().textProperty().addListener((obs, oldVal, newVal) -> validate.run());
            sp.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    try {
                        sp.commitValue();
                    } catch (Exception ignored) {
                    }
                    validate.run();
                }
            });
        }

        dialog.setOnShown(e -> {
            Node cancelBtn = dialog.getDialogPane().lookupButton(hiddenCancelType);
            if (cancelBtn != null) {
                cancelBtn.setVisible(false);
                cancelBtn.setManaged(false);
            }
            validate.run();
        });

        dialog.setResultConverter(button -> {
            if (button == okButtonType) {
                int w = getSpinnerValue(widthSpinner, 50);
                int h = getSpinnerValue(heightSpinner, 50);
                int p = getSpinnerValue(plantsSpinner, 900);
                int herb = getSpinnerValue(herbivoresSpinner, 180);
                int pred = getSpinnerValue(predatorsSpinner, 70);

                if (w >= 1 && w <= 100 && h >= 1 && h <= 100 && p >= 0 && herb >= 0 && pred >= 0 && ((long) p + herb + pred <= (long) w * h)) {
                    return new int[]{w, h, p, herb, pred};
                }
            }
            return null;
        });

        Optional<int[]> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private int getSpinnerValue(Spinner<Integer> spinner, int fallback) {
        try {
            spinner.commitValue();
            return spinner.getValue();
        } catch (Exception e) {
            try {
                return Integer.parseInt(spinner.getEditor().getText().trim());
            } catch (Exception ex) {
                return fallback;
            }
        }
    }

    private void initEmojiTextures() {
        plantImage = createEmojiImage("🌿");
        herbivoreImage = createEmojiImage("🐰");
        predatorImage = createEmojiImage("🐺");
    }

    private Image createEmojiImage(String emoji) {
        try {
            Text text = new Text(emoji);
            Font emojiFont = Font.font("Segoe UI Emoji", 48);
            if (!"Segoe UI Emoji".equalsIgnoreCase(emojiFont.getFamily())) {
                emojiFont = Font.font(48);
            }
            text.setFont(emojiFont);

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            return text.snapshot(params, null);
        } catch (Exception e) {
            return null;
        }
    }

    private VBox createTopPanel() {
        stepButton = new Button("Сделать ход");
        stepButton.setMinWidth(150);
        stepButton.setPrefWidth(150);
        stepButton.setOnAction(e -> doManualSteps());

        autoButton = new Button("Запустить авто");
        autoButton.setMinWidth(120);
        autoButton.setOnAction(e -> toggleAutoMode());

        Label speedTitle = new Label("Скорость:");
        Slider speedSlider = new Slider(1, 10, 1);
        speedSlider.setBlockIncrement(1);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setSnapToTicks(true);
        speedSlider.setMinWidth(120);
        speedSlider.setPrefWidth(140);
        speedSlider.setMaxWidth(200);
        speedSlider.setShowTickMarks(false);
        speedSlider.setShowTickLabels(false);

        Label speedValLabel = new Label("1x");
        speedValLabel.setStyle("-fx-font-weight: bold;");
        speedValLabel.setMinWidth(35);

        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int speed = (int) Math.round(newVal.doubleValue());
            stepMultiplier = speed;

            speedValLabel.setText(speed + "x");
            stepButton.setText(stepMultiplier == 1 ? "Сделать ход" : "Сделать ход (×" + stepMultiplier + ")");

            if (autoTimeline != null) {
                autoTimeline.setRate(speed);
            }
        });

        ToolBar controlsBar = new ToolBar(
                stepButton,
                autoButton,
                new Separator(Orientation.VERTICAL),
                speedTitle,
                speedSlider,
                speedValLabel
        );
        controlsBar.setPadding(new Insets(5, 8, 5, 8));

        stepLabel = new Label("Ход: 0");
        stepLabel.setStyle("-fx-font-weight: bold;");

        plantsLabel = new Label("🌿 Трава: 0");
        plantsLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");

        herbivoresLabel = new Label("🐰 Травоядные: 0");
        herbivoresLabel.setStyle("-fx-text-fill: #1565c0; -fx-font-weight: bold;");

        predatorsLabel = new Label("🐺 Хищники: 0");
        predatorsLabel.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");

        ToolBar statsBar = new ToolBar(
                stepLabel,
                new Separator(Orientation.VERTICAL),
                plantsLabel,
                herbivoresLabel,
                predatorsLabel
        );
        statsBar.setPadding(new Insets(4, 8, 4, 8));

        return new VBox(controlsBar, statsBar);
    }

    private void doManualSteps() {
        for (int i = 0; i < stepMultiplier; i++) {
            if (isGameOver) {
                break;
            }
            doStep();
        }
    }

    private void doStep() {
        if (isGameOver) {
            return;
        }

        turnCount++;
        List<Agent> agents = new ArrayList<>(environment.getAvailableAgents());
        for (Agent agent : agents) {
            agent.turn();
        }

        updateStats();
        drawField();
        checkGameOver();
    }

    private void toggleAutoMode() {
        if (autoTimeline.getStatus() == Animation.Status.RUNNING) {
            autoTimeline.stop();
            autoButton.setText("Запустить авто");
            stepButton.setDisable(false);
        } else {
            autoTimeline.play();
            autoButton.setText("Остановить авто");
            stepButton.setDisable(true);
        }
    }

    private void updateStats() {
        List<Agent> agents = environment.getAvailableAgents();
        long plants = 0;
        long herbivores = 0;
        long predators = 0;

        for (Agent a : agents) {
            if (a instanceof Plant) plants++;
            else if (a instanceof Herbivore) herbivores++;
            else if (a instanceof Predator) predators++;
        }

        stepLabel.setText("Ход: " + turnCount);
        plantsLabel.setText("🌿 Трава: " + plants);
        herbivoresLabel.setText("🐰 Травоядные: " + herbivores);
        predatorsLabel.setText("🐺 Хищники: " + predators);
    }

    private void checkGameOver() {
        List<Agent> agents = environment.getAvailableAgents();
        long plants = 0;
        long herbivores = 0;
        long predators = 0;

        for (Agent a : agents) {
            if (a instanceof Plant) plants++;
            else if (a instanceof Herbivore) herbivores++;
            else if (a instanceof Predator) predators++;
        }

        if (plants == 0 || herbivores == 0 || predators == 0) {
            isGameOver = true;
            if (autoTimeline != null) {
                autoTimeline.stop();
            }
            stepButton.setDisable(true);
            autoButton.setDisable(true);

            final long finalPlants = plants;
            final long finalHerbivores = herbivores;
            final long finalPredators = predators;
            final int finalTurn = turnCount;

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Конец игры");
                alert.setHeaderText("Один из видов полностью вымер!");
                alert.setGraphic(null);

                alert.setContentText(String.format(
                        """
                                Симуляция остановлена на шаге: %d
                                
                                Размер популяций:
                                🌿 Растения: %d
                                🐰 Травоядные: %d
                                🐺 Хищники: %d""",
                        finalTurn, finalPlants, finalHerbivores, finalPredators
                ));

                ButtonType viewMapButton = new ButtonType("Посмотреть карту", ButtonBar.ButtonData.CANCEL_CLOSE);
                ButtonType exitGameButton = new ButtonType("Завершить игру", ButtonBar.ButtonData.OK_DONE);

                alert.getButtonTypes().setAll(viewMapButton, exitGameButton);

                alert.showAndWait().ifPresent(response -> {
                    if (response == exitGameButton) {
                        Platform.exit();
                        System.exit(0);
                    }
                });
            });
        }
    }

    private void drawField() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double margin = 8.0;
        double availW = w - (margin * 2);
        double availH = h - (margin * 2);
        if (availW <= 0 || availH <= 0) return;

        int envW = environment.getWidth();
        int envH = environment.getHeight();

        double cellSize = Math.min(availW / envW, availH / envH);
        double fieldW = cellSize * envW;
        double fieldH = cellSize * envH;

        double startX = (w - fieldW) / 2.0;
        double startY = (h - fieldH) / 2.0;

        for (int y = 0; y < envH; y++) {
            for (int x = 0; x < envW; x++) {
                double px = startX + x * cellSize;
                double py = startY + y * cellSize;

                Agent agent = environment.getAgent(x, y);

                Color cellBgColor;
                Color cellBorderColor;
                Image emojiImg = null;
                String emojiFallback = "";

                switch (agent) {
                    case Plant ignored -> {
                        cellBgColor = COLOR_PLANT_BG;
                        cellBorderColor = COLOR_PLANT_BORDER;
                        emojiImg = plantImage;
                        emojiFallback = "🌿";
                    }
                    case Herbivore ignored -> {
                        cellBgColor = COLOR_HERBIVORE_BG;
                        cellBorderColor = COLOR_HERBIVORE_BORDER;
                        emojiImg = herbivoreImage;
                        emojiFallback = "🐰";
                    }
                    case Predator ignored -> {
                        cellBgColor = COLOR_PREDATOR_BG;
                        cellBorderColor = COLOR_PREDATOR_BORDER;
                        emojiImg = predatorImage;
                        emojiFallback = "🐺";
                    }
                    case null, default -> {
                        cellBgColor = COLOR_EMPTY_BG;
                        cellBorderColor = COLOR_GRID;
                    }
                }

                gc.setFill(cellBgColor);
                gc.fillRect(px, py, cellSize, cellSize);

                gc.setStroke(cellBorderColor);
                gc.setLineWidth(0.5);
                gc.strokeRect(px, py, cellSize, cellSize);

                if (agent != null) {
                    if (emojiImg != null) {
                        double imgW = emojiImg.getWidth();
                        double imgH = emojiImg.getHeight();
                        double scale = Math.min(cellSize / imgW, cellSize / imgH) * 0.90;
                        double dw = imgW * scale;
                        double dh = imgH * scale;
                        gc.drawImage(emojiImg, px + (cellSize - dw) / 2.0, py + (cellSize - dh) / 2.0, dw, dh);
                    } else {
                        gc.setFill(Color.BLACK);
                        gc.setFont(Font.font("Segoe UI Emoji", Math.max(7.0, cellSize * 0.85)));
                        gc.setTextAlign(TextAlignment.CENTER);
                        gc.setTextBaseline(VPos.CENTER);
                        gc.fillText(emojiFallback, px + (cellSize / 2.0), py + (cellSize / 2.0));
                    }
                }
            }
        }

        gc.setStroke(Color.web("#9e9e9e"));
        gc.setLineWidth(1.0);
        gc.strokeRect(startX, startY, fieldW, fieldH);
    }
}