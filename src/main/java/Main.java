import agent.Agent;
import agent.Herbivore;
import agent.Plant;
import agent.Predator;
import environment.Environment;

import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final int TURNS = 500000;
    private static final Environment environment = new Environment();

    public static void main(String[] args) {
        environment.initGame();
        environment.drawField();

        for (int i = 1; i <= TURNS; i++) {
            environment.spawnBackgroundPlants();

            List<Agent> tmpList = new ArrayList<>(environment.getAvailableAgents());

            long plants = tmpList.stream().filter(a -> a instanceof Plant).count();
            long herbivores = tmpList.stream().filter(a -> a instanceof Herbivore).count();
            long predators = tmpList.stream().filter(a -> a instanceof Predator).count();

            if (i % 100 == 0 || i == TURNS - 1) {
                System.out.printf("Ход %4d | Растений: %4d, Травоядных: %3d, Хищников: %3d%n",
                        i, plants, herbivores, predators);
            }

            for (Agent agent : tmpList) {
                agent.turn();
            }
        }

        environment.drawField();
    }
}