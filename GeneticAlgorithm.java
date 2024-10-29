import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GeneticAlgorithm {
    private static final int POPULATION_SIZE = 10000;
    private static final int GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.01;
    private static final String SEQUENCE = "HPHPPHHPHPPHPHHPPHPH";
    private static final String CSV_FILE = "log.csv";

    public static void main(String[] args) throws IOException {
        // Initialize the population with random solutions
        List<HPModel> population = initializePopulation();
        HPModel bestSolution = null;

        // Create a CSV file to log the results of each generation
        try (FileWriter writer = new FileWriter(CSV_FILE)) {
            writer.write("Generation,AverageFitness,BestFitness,BestOverallFitness,HydrophobicContacts,Overlaps\n");

            // Create a thread pool for parallel execution
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            // Run the genetic algorithm for a specified number of generations
            for (int generation = 0; generation < GENERATIONS; generation++) {
                System.out.println("Generation " + generation);

                // Evaluate the fitness of each individual in the population in parallel
                List<Future<?>> futures = new ArrayList<>();
                for (HPModel model : population) {
                    futures.add(executor.submit(() -> model.calculateFitnessScore()));
                }
                // Wait for all fitness evaluations to complete
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        System.err.println("Error: " + e.getMessage());
                    }
                }

                // Find the best solution in the current generation
                HPModel bestInGeneration = Collections.max(population, (a, b) -> Double.compare(a.calculateFitnessScore(), b.calculateFitnessScore()));
                if (bestSolution == null || bestInGeneration.calculateFitnessScore() > bestSolution.calculateFitnessScore()) {
                    bestSolution = bestInGeneration;
                }

                // Calculate the average fitness of the current generation
                double averageFitness = population.stream().mapToDouble(HPModel::calculateFitnessScore).average().orElse(0.0);

                // Log the results of the current generation to the CSV file
                writer.write(String.format("%d,%.2f,%.2f,%.2f,%d,%d\n",
                        generation,
                        averageFitness,
                        bestInGeneration.calculateFitnessScore(),
                        bestSolution.calculateFitnessScore(),
                        bestSolution.calculateEnergy(),
                        bestSolution.countOverlaps()));

            
                //clearScreen();
                //bestSolution.printGrid();

                // Create a new generation of solutions in parallel
                List<HPModel> newGeneration = new ArrayList<>();
                futures.clear();
                for (int i = 0; i < POPULATION_SIZE; i++) {
                    final List<HPModel> finalPopulation = population;
                    futures.add(executor.submit(() -> {
                        HPModel parent1 = selectParent(finalPopulation);
                        HPModel parent2 = selectParent(finalPopulation);
                        HPModel offspring = crossover(parent1, parent2, new Random());
                        mutate(offspring, new Random());
                        synchronized (newGeneration) {
                            newGeneration.add(offspring);
                        }
                    }));
                }
                // Wait for all new generation creations to complete
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        System.err.println("Error: " + e.getMessage());
                    }
                }

                population = newGeneration;
            }

            executor.shutdown();
        }

        // Print the best solution found
        if (bestSolution != null) {
            System.out.println("Best Solution:");
            System.out.println("Moves: " + bestSolution.getMoves());
            System.out.println("Fitness Score: " + bestSolution.calculateFitnessScore());
            System.out.println("Energy: " + bestSolution.calculateEnergy());
            System.out.println("Overlaps: " + bestSolution.countOverlaps());
        }
    }

    // Initialize the population with random moves
    private static List<HPModel> initializePopulation() {
        List<HPModel> population = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            String moves = randomMoves(SEQUENCE.length() - 1, random);
            population.add(new HPModel(SEQUENCE, moves));
        }
        return population;
    }

    // Generate a random sequence of moves
    private static String randomMoves(int length, Random random) {
        char[] moves = new char[length];
        char[] possibleMoves = {'U', 'D', 'L', 'R'};
        for (int i = 0; i < length; i++) {
            moves[i] = possibleMoves[random.nextInt(possibleMoves.length)];
        }
        return new String(moves);
    }

    // Select a random parent from the population
    private static HPModel selectParent(List<HPModel> population) {
        Random random = new Random();
        return population.get(random.nextInt(population.size()));
    }

    // Perform crossover between two parents to create a new offspring
    private static HPModel crossover(HPModel parent1, HPModel parent2, Random random) {
        String moves1 = parent1.getMoves();
        String moves2 = parent2.getMoves();
        char[] newMoves = new char[moves1.length()];

        // Randomly choose moves from either parent1 or parent2
        for (int i = 0; i < moves1.length(); i++) {
            newMoves[i] = random.nextBoolean() ? moves1.charAt(i) : moves2.charAt(i);
        }

        // Return a new HPModel with the combined moves
        return new HPModel(SEQUENCE, new String(newMoves));
    }

    // Apply mutation to the model's moves
    private static void mutate(HPModel model, Random random) {
        char[] moves = model.getMoves().toCharArray();
        char[] possibleMoves = {'U', 'D', 'L', 'R'};

        // Randomly change some moves based on the mutation rate
        for (int i = 0; i < moves.length; i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                moves[i] = possibleMoves[random.nextInt(possibleMoves.length)];
            }
        }

        // Update the model with the new mutated moves
        model.setMoves(new String(moves));
    }

    public static void clearScreen() {
        try {
            String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            System.err.println("Error clearing screen: " + e.getMessage());
        }
    }
}

