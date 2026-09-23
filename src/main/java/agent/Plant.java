package agent;

import environment.Environment;
import environment.Point;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Plant extends Agent {

    public Plant(int x, int y, Environment environment) {
        super(x, y, 10, 0, 5, "Р", environment);
    }

    @Override
    public void turn() {
        if (!isAlive()) {
            return;
        }

        energy += energyIncrease;

        if (energy >= initEnergy * 2) {
            List<Point> emptyCells = environment.getEmptyNeighborCells(x, y, 1);
            if (!emptyCells.isEmpty()) {
                Point cell = emptyCells.get(ThreadLocalRandom.current().nextInt(emptyCells.size()));
                environment.addAgent(createChild(cell.x(), cell.y(), environment));
                energy -= initEnergy;
            } else {
                energy = initEnergy * 2;
            }
        }
    }

    @Override
    protected void move(Point point) {}

    @Override
    protected Agent createChild(int x, int y, Environment environment) {
        return new Plant(x, y, environment);
    }

    @Override
    public boolean canEat(Agent other) {
        return false;
    }
}