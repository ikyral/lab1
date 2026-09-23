package agent;

import environment.Environment;

import java.util.List;

public class Predator extends Agent {

    public Predator(int x, int y, Environment environment) {
        super(x, y, 100, 1, 60, "Х", environment);
    }

    @Override
    public void turn() {
        super.turn();
        if (!isAlive()) {
            return;
        }

        Herbivore nearestHerbivore = findNearestHerbivore(environment.getNeighbors(x, y, VISION_AREA));
        if (nearestHerbivore != null) {
            move(nearestHerbivore.getPoint());
            return;
        }

        wander(environment.getEmptyNeighborCells(x, y, 1));
    }

    @Override
    protected Agent createChild(int x, int y, Environment environment) {
        return new Predator(x, y, environment);
    }

    @Override
    public boolean canEat(Agent other) {
        return other instanceof Herbivore;
    }

    private Herbivore findNearestHerbivore(List<Agent> agents) {
        Herbivore nearest = null;
        int minDistance = Integer.MAX_VALUE;
        for (Agent agent : agents) {
            if (agent instanceof Herbivore herbivore) {
                int dist = Math.abs(x - herbivore.getPoint().x()) + Math.abs(y - herbivore.getPoint().y());
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = herbivore;
                }
            }
        }

        return nearest;
    }
}