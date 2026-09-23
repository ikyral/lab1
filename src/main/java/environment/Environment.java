package environment;

import agent.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Environment {
    public static final int SIDE = 60;
    private static final Random randomGen = new Random();

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String COLOR_PLANT = "\u001B[38;2;76;175;80m";
    private static final String COLOR_HERBIVORE = "\u001B[38;2;30;136;229m";
    private static final String COLOR_PREDATOR = "\u001B[38;2;229;57;53m";

    private final int width;
    private final int height;
    private final List<Agent> availableAgents = new ArrayList<>();
    private final Agent[][] field;

    public Environment() {
        this(SIDE, SIDE);
    }

    public Environment(int width, int height) {
        this.width = Math.clamp(width, 1, 100);
        this.height = Math.clamp(height, 1, 100);
        this.field = new Agent[this.height][this.width];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<Agent> getAvailableAgents() {
        return new ArrayList<>(availableAgents);
    }

    public Agent getAgent(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return field[y][x];
    }

    public void setAgent(int x, int y, Agent agent) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            field[y][x] = agent;
        }
    }

    public void addAgent(Agent agent) {
        availableAgents.add(agent);
        setAgent(agent.getPoint().x(), agent.getPoint().y(), agent);
    }

    public void deleteAgent(Agent agent) {
        availableAgents.remove(agent);
        Point point = agent.getPoint();
        if (point.x() >= 0 && point.x() < width && point.y() >= 0 && point.y() < height) {
            if (field[point.y()][point.x()] == agent) {
                field[point.y()][point.x()] = null;
            }
        }
    }

    public List<Agent> getNeighbors(int x, int y, int radius) {
        List<Agent> neighbors = new ArrayList<>();
        int minY = Math.max(y - radius, 0);
        int maxY = Math.min(y + radius, height - 1);
        int minX = Math.max(x - radius, 0);
        int maxX = Math.min(x + radius, width - 1);

        for (int i = minY; i <= maxY; i++) {
            for (int j = minX; j <= maxX; j++) {
                if (y == i && x == j) {
                    continue;
                }

                Agent neighbor = field[i][j];
                if (neighbor != null) {
                    neighbors.add(neighbor);
                }
            }
        }

        return neighbors;
    }

    public List<Point> getEmptyNeighborCells(int x, int y, int radius) {
        List<Point> emptyCells = new ArrayList<>();
        int minY = Math.max(y - radius, 0);
        int maxY = Math.min(y + radius, height - 1);
        int minX = Math.max(x - radius, 0);
        int maxX = Math.min(x + radius, width - 1);

        for (int i = minY; i <= maxY; i++) {
            for (int j = minX; j <= maxX; j++) {
                if (i == y && j == x) {
                    continue;
                }

                if (field[i][j] == null) {
                    emptyCells.add(new Point(j, i));
                }
            }
        }

        return emptyCells;
    }

    public void moveAgent(Agent agent, int newX, int newY) {
        Point pt = agent.getPoint();
        if (pt.x() >= 0 && pt.x() < width && pt.y() >= 0 && pt.y() < height) {
            field[pt.y()][pt.x()] = null;
        }
        agent.setX(newX);
        agent.setY(newY);
        field[newY][newX] = agent;
    }

    public boolean isCellWalkable(Agent agent, int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }

        Agent target = field[y][x];
        if (target == null) {
            return true;
        }

        return agent != null && agent.canEat(target);
    }

    public void initGame(int plants, int herbivores, int predators) {
        addAgents(Type.PLANT, plants);
        addAgents(Type.HERBIVORE, herbivores);
        addAgents(Type.PREDATOR, predators);
    }

    public void drawField() {
        System.out.println("\n");
        for (Agent[] row : field) {
            for (Agent agent : row) {
                if (agent == null) {
                    System.out.print(". ");
                } else {
                    System.out.print(colorFor(agent) + agent.getSymbol() + ANSI_RESET + " ");
                }
            }
            System.out.println();
        }
        System.out.println("\n");
    }

    private String colorFor(Agent agent) {
        if (agent instanceof Plant) {
            return COLOR_PLANT;
        } else if (agent instanceof Herbivore) {
            return COLOR_HERBIVORE;
        } else if (agent instanceof Predator) {
            return COLOR_PREDATOR;
        }
        return "";
    }

    private void addAgents(Type type, int amount) {
        int maxAttempts = amount * 100;
        int placed = 0;

        for (int i = 0; i < maxAttempts && placed < amount; i++) {
            int x = randomGen.nextInt(width);
            int y = randomGen.nextInt(height);

            if (this.isCellWalkable(null, x, y)) {
                Agent agent = switch (type) {
                    case PLANT -> new Plant(x, y, this);
                    case PREDATOR -> new Predator(x, y, this);
                    case HERBIVORE -> new Herbivore(x, y, this);
                };

                this.setAgent(x, y, agent);
                availableAgents.add(agent);
                placed++;
            }
        }
    }
}
