package agent;

import environment.Environment;
import environment.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Herbivore extends Agent {
    private static final double ESCAPE_PROBABILITY = 0.40;

    public Herbivore(int x, int y, Environment environment) {
        super(x, y, 40, 1, 6, "Т", environment);
    }

    @Override
    public void turn() {
        super.turn();
        if (!isAlive()) {
            return;
        }

        Point nextPoint = null;
        Predator nearestPredator = findNearestPredator(environment.getNeighbors(x, y, VISION_AREA));

        if (nearestPredator != null) {
            int dist = Math.abs(x - nearestPredator.getPoint().x()) + Math.abs(y - nearestPredator.getPoint().y());
            if (dist <= 1 || ThreadLocalRandom.current().nextDouble() < ESCAPE_PROBABILITY) {
                nextPoint = chooseEscapePoint(nearestPredator, environment);
            }
        }

        if (nextPoint == null) {
            Plant nearestPlant = findNearestPlant(environment.getNeighbors(x, y, VISION_AREA));
            if (nearestPlant != null) {
                nextPoint = nearestPlant.getPoint();
            }
        }

        if (nextPoint != null) {
            move(nextPoint);
        } else {
            wander(environment.getEmptyNeighborCells(x, y, 1));
        }
    }

    @Override
    protected Agent createChild(int x, int y, Environment environment) {
        return new Herbivore(x, y, environment);
    }

    @Override
    public boolean canEat(Agent other) {
        return other instanceof Plant;
    }

    private Predator findNearestPredator(List<Agent> agents) {
        Predator nearest = null;
        int minDistance = Integer.MAX_VALUE;
        for (Agent agent : agents) {
            if (agent instanceof Predator predator) {
                int dist = Math.abs(x - predator.getPoint().x()) + Math.abs(y - predator.getPoint().y());
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = predator;
                }
            }
        }

        return nearest;
    }

    private Plant findNearestPlant(List<Agent> agents) {
        Plant nearest = null;
        int minDistance = Integer.MAX_VALUE;
        for (Agent agent : agents) {
            if (agent instanceof Plant plant) {
                int dist = Math.abs(x - plant.getPoint().x()) + Math.abs(y - plant.getPoint().y());
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = plant;
                }
            }
        }

        return nearest;
    }

    private Point chooseEscapePoint(Predator predator, Environment environment) {
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        int maxDistance = Math.abs(x - predator.getPoint().x()) + Math.abs(y - predator.getPoint().y());
        List<Point> escapeMoves = new ArrayList<>();

        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (environment.isCellWalkable(this, nx, ny)) {
                int dist = Math.abs(nx - predator.getPoint().x()) + Math.abs(ny - predator.getPoint().y());
                if (dist > maxDistance) {
                    maxDistance = dist;
                    escapeMoves.clear();
                    escapeMoves.add(new Point(nx, ny));
                } else if (dist == maxDistance) {
                    escapeMoves.add(new Point(nx, ny));
                }
            }
        }

        if (!escapeMoves.isEmpty()) {
            return escapeMoves.get(ThreadLocalRandom.current().nextInt(escapeMoves.size()));
        }

        return null;
    }
}