package agent;

import environment.Environment;
import environment.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Agent {
    protected static final int VISION_AREA = 2;
    private static final double DIRECTION_CHANGE_PROBABILITY = 0.15;

    protected final int energyDecrease;
    protected final int energyIncrease;
    protected final int initEnergy;

    protected final String symbol;
    protected final Environment environment;

    protected int x;
    protected int y;
    protected int energy;
    private boolean alive = true;
    private int lastDx = 0;
    private int lastDy = 0;

    protected Agent(int x, int y, int initEnergy, int energyDecrease, int energyIncrease,
                    String symbol, Environment environment) {
        this.x = x;
        this.y = y;
        this.initEnergy = initEnergy;
        this.energy = initEnergy;
        this.energyDecrease = energyDecrease;
        this.energyIncrease = energyIncrease;
        this.environment = environment;
        this.symbol = symbol;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Point getPoint() {
        return new Point(x, y);
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isAlive() {
        return alive;
    }

    public void turn() {
        if (!alive) {
            return;
        }

        decreaseEnergy();
        if (energy <= 0) {
            die();
        }
    }

    public void die() {
        alive = false;
        environment.deleteAgent(this);
    }

    protected void move(Point target) {
        if (!alive || (this.x == target.x() && this.y == target.y())) {
            return;
        }

        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        List<Point> bestMoves = new ArrayList<>();
        int bestDistance = Integer.MAX_VALUE;

        for (int[] dir : directions) {
            int nx = this.x + dir[0];
            int ny = this.y + dir[1];
            if (environment.isCellWalkable(this, nx, ny)) {
                int dist = Math.abs(nx - target.x()) + Math.abs(ny - target.y());
                if (dist < bestDistance) {
                    bestDistance = dist;
                    bestMoves.clear();
                    bestMoves.add(new Point(nx, ny));
                } else if (dist == bestDistance) {
                    bestMoves.add(new Point(nx, ny));
                }
            }
        }

        if (!bestMoves.isEmpty()) {
            Point chosen = bestMoves.get(ThreadLocalRandom.current().nextInt(bestMoves.size()));
            int nextX = chosen.x();
            int nextY = chosen.y();

            Agent targetAgent = environment.getAgent(nextX, nextY);
            if (targetAgent != null && canEat(targetAgent)) {
                targetAgent.die();
                environment.moveAgent(this, nextX, nextY);
                increaseEnergy();
            } else {
                environment.moveAgent(this, nextX, nextY);
            }
        }
    }

    protected void decreaseEnergy() {
        energy -= energyDecrease;
    }

    protected void increaseEnergy() {
        energy += energyIncrease;

        if (energy >= initEnergy * 2) {
            multiply();
        }

        if (energy > initEnergy * 2) {
            energy = initEnergy * 2;
        }
    }

    protected void multiply() {
        List<Point> emptyCells = environment.getEmptyNeighborCells(x, y, 1);
        if (!emptyCells.isEmpty()) {
            this.energy -= this.initEnergy;
            Point emptyCell = emptyCells.get(ThreadLocalRandom.current().nextInt(emptyCells.size()));
            Agent child = createChild(emptyCell.x(), emptyCell.y(), environment);
            environment.addAgent(child);
        }
    }

    protected void wander(List<Point> emptyCells) {
        if (emptyCells.isEmpty()) {
            return;
        }

        Point forwardCell = null;
        if (lastDx != 0 || lastDy != 0) {
            int desiredX = this.x + lastDx;
            int desiredY = this.y + lastDy;
            for (Point cell : emptyCells) {
                if (cell.x() == desiredX && cell.y() == desiredY) {
                    forwardCell = cell;
                    break;
                }
            }
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (forwardCell != null && random.nextDouble() >= DIRECTION_CHANGE_PROBABILITY) {
            move(forwardCell);
            return;
        }

        Point chosenCell = emptyCells.get(random.nextInt(emptyCells.size()));
        this.lastDx = chosenCell.x() - this.x;
        this.lastDy = chosenCell.y() - this.y;
        move(chosenCell);
    }

    public abstract boolean canEat(Agent other);

    protected abstract Agent createChild(int x, int y, Environment environment);
}