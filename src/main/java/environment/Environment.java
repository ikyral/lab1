package environment;

import agent.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Environment {
    public static final int SIDE = 60;
    private static final Random randomGen = new Random();

    private final List<Agent> availableAgents = new ArrayList<>();
    private final Agent[][] field = new Agent[SIDE][SIDE];

    public List<Agent> getAvailableAgents() {
        return new ArrayList<>(availableAgents);
    }

    public Agent getAgent(int x, int y) {
        return field[y][x];
    }

    public void setAgent(int x, int y, Agent agent) {
        field[y][x] = agent;
    }

    public void addAgent(Agent agent) {
        availableAgents.add(agent);
        setAgent(agent.getPoint().x(), agent.getPoint().y(), agent);
    }

    public void deleteAgent(Agent agent) {
        availableAgents.remove(agent);
        Point point = agent.getPoint();
        if (field[point.y()][point.x()] == agent) {
            field[point.y()][point.x()] = null;
        }
    }

    public List<Agent> getNeighbors(int x, int y, int radius) {
        List<Agent> neighbors = new ArrayList<>();
        int minY = Math.max(y - radius, 0);
        int maxY = Math.min(y + radius, SIDE - 1);
        int minX = Math.max(x - radius, 0);
        int maxX = Math.min(x + radius, SIDE - 1);

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
        int maxY = Math.min(y + radius, SIDE - 1);
        int minX = Math.max(x - radius, 0);
        int maxX = Math.min(x + radius, SIDE - 1);

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
        field[agent.getPoint().y()][agent.getPoint().x()] = null;
        agent.setX(newX);
        agent.setY(newY);
        field[newY][newX] = agent;
    }

    public boolean isCellWalkable(Agent agent, int x, int y) {
        if (x < 0 || x >= SIDE || y < 0 || y >= SIDE) {
            return false;
        }

        Agent target = field[y][x];
        if (target == null) {
            return true;
        }

        return agent != null && agent.canEat(target);
    }

    public void initGame() {
        addAgents(Type.PLANT, 1300);
        addAgents(Type.HERBIVORE, 1);
        addAgents(Type.PREDATOR, 23);
    }

    private void addAgents(Type type, int amount) {
        for (int i = 0; i < amount; i++) {
            int x;
            int y;
            do {
                x = randomGen.nextInt(SIDE);
                y = randomGen.nextInt(SIDE);
            } while (!this.isCellWalkable(null, x, y));

            Agent agent = switch (type) {
                case PLANT -> new Plant(x, y, this);
                case PREDATOR -> new Predator(x, y, this);
                case HERBIVORE -> new Herbivore(x, y, this);
            };

            this.setAgent(x, y, agent);
            availableAgents.add(agent);
        }
    }
}