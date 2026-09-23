package app;

import agent.Agent;
import agent.Herbivore;
import agent.Plant;
import agent.Predator;
import environment.Environment;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class ConsoleApp {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String COLOR_PLANT = "\u001B[38;2;46;125;50m";
    private static final String COLOR_HERBIVORE = "\u001B[38;2;21;101;192m";
    private static final String COLOR_PREDATOR = "\u001B[38;2;198;40;40m";

    private static Environment environment;
    private static int turn = 0;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println(BOLD + "Искусственная жизнь" + RESET);
        environment = configureEnvironment(scanner);
        turn = 0;

        printStats();
        environment.drawField();

        boolean exit = false;
        while (!exit) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    boolean gameOver = doTurn();
                    printStats();
                    environment.drawField();
                    if (gameOver) {
                        printGameOverMessage();
                        exit = true;
                    }
                }
                case "2" -> {
                    int steps = askInt(scanner, "Сколько ходов сделать", 10, 1, 100000);
                    boolean gameOver = false;
                    for (int i = 0; i < steps && !gameOver; i++) {
                        gameOver = doTurn();
                        printStats();
                        environment.drawField();
                    }
                    if (gameOver) {
                        printGameOverMessage();
                        exit = true;
                    }
                }
                case "3" -> {
                    int delay = askInt(scanner, "Задержка между ходами, мс", 200, 0, 5000);
                    boolean gameOver = runAutoMode(scanner, delay);
                    if (gameOver) {
                        printGameOverMessage();
                        exit = true;
                    }
                }
                case "4" -> {
                    printStats();
                    environment.drawField();
                }
                case "0" -> exit = true;
                default -> System.out.println("Неизвестная команда, попробуйте снова.");
            }
        }
    }

    private static Environment configureEnvironment(Scanner scanner) {
        while (true) {
            int width = askInt(scanner, "Ширина поля [1; 100]", 50, 1, 100);
            int height = askInt(scanner, "Высота поля [1; 100]", 50, 1, 100);
            int plants = askInt(scanner, "🌿 Количество растений", 900, 0, 10000);
            int herbivores = askInt(scanner, "🐰 Количество травоядных", 180, 0, 10000);
            int predators = askInt(scanner, "🐺 Количество хищников", 70, 0, 10000);

            long totalCells = (long) width * height;
            long totalAgents = (long) plants + herbivores + predators;

            if (totalAgents > totalCells) {
                System.out.println("Агентов (" + totalAgents + ") больше, чем клеток на поле (" + totalCells + "). Задайте параметры ещё раз.\n");
                continue;
            }

            Environment env = new Environment(width, height);
            env.initGame(plants, herbivores, predators);
            System.out.println("Всего агентов: " + totalAgents + " / " + totalCells + " клеток\n");
            return env;
        }
    }

    private static int askInt(Scanner scanner, String prompt, int defaultValue, int min, int max) {
        while (true) {
            System.out.print(prompt + " (по умолчанию " + defaultValue + "): ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                return defaultValue;
            }

            try {
                int value = Integer.parseInt(line);
                if (value < min || value > max) {
                    System.out.println("Значение должно быть в диапазоне [" + min + "; " + max + "].");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Введите целое число.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n1 — Сделать один ход");
        System.out.println("2 — Сделать несколько ходов");
        System.out.println("3 — Автоматический режим (до Enter или вымирания вида)");
        System.out.println("4 — Показать текущее поле и статистику");
        System.out.println("0 — Выход");
        System.out.print("Выберите действие: ");
    }

    private static boolean doTurn() {
        turn++;
        List<Agent> agents = environment.getAvailableAgents();
        for (Agent agent : agents) {
            agent.turn();
        }
        return isGameOver();
    }

    private static boolean runAutoMode(Scanner scanner, int delayMs) {
        System.out.println("Автоматический режим запущен. Нажмите Enter, чтобы остановить.\n");

        boolean gameOver = false;
        boolean stopRequested = false;

        while (!stopRequested) {
            gameOver = doTurn();
            printStats();
            environment.drawField();

            if (gameOver) {
                break;
            }

            long deadline = System.currentTimeMillis() + delayMs;
            while (System.currentTimeMillis() < deadline) {
                try {
                    if (System.in.available() > 0) {
                        stopRequested = true;
                        break;
                    }
                    Thread.sleep(20);
                } catch (IOException e) {
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (stopRequested) {
            try {
                scanner.nextLine();
            } catch (Exception ignored) {
            }
        }

        return gameOver;
    }

    private static boolean isGameOver() {
        int[] counts = countAgents();
        return counts[0] == 0 || counts[1] == 0 || counts[2] == 0;
    }

    private static int[] countAgents() {
        int plants = 0;
        int herbivores = 0;
        int predators = 0;

        for (Agent agent : environment.getAvailableAgents()) {
            if (agent instanceof Plant) {
                plants++;
            } else if (agent instanceof Herbivore) {
                herbivores++;
            } else if (agent instanceof Predator) {
                predators++;
            }
        }

        return new int[]{plants, herbivores, predators};
    }

    private static void printStats() {
        int[] counts = countAgents();
        System.out.println(BOLD + "Ход: " + turn + RESET
                + "   " + COLOR_PLANT + "🌿 Трава: " + counts[0] + RESET
                + "   " + COLOR_HERBIVORE + "🐰 Травоядные: " + counts[1] + RESET
                + "   " + COLOR_PREDATOR + "🐺 Хищники: " + counts[2] + RESET);
    }

    private static void printGameOverMessage() {
        int[] counts = countAgents();
        System.out.println("\n" + BOLD + "Один из видов полностью вымер! Симуляция остановлена на шаге: " + turn + RESET);
        System.out.println(COLOR_PLANT + "🌿 Растения: " + counts[0] + RESET);
        System.out.println(COLOR_HERBIVORE + "🐰 Травоядные: " + counts[1] + RESET);
        System.out.println(COLOR_PREDATOR + "🐺 Хищники: " + counts[2] + RESET);
    }
}