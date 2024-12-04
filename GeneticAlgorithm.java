import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;

public class GeneticAlgorithm {
    private static final int POPULATION_SIZE = 200;
    private static final int GENERATIONS = 1000;
    private static final double MUTATION_RATE = 0.1;
    private static final String CSV_FILE = "log.csv";

    public static void main(String[] args) throws IOException {
        // Test the algorithm with benchmark sequences
        testWithBenchmarks();
    }

    private static void testWithBenchmarks() throws IOException {
        String[] benchmarks = {
            //Examples.SEQ20, // Best score: 9/9
            //Examples.SEQ24, // Best score: 7/9
            //Examples.SEQ25, // Best score: 7/8
            //Examples.SEQ36, // Best score: 10/14
            Examples.SEQ48, // Best score: 14/22
            //Examples.SEQ50, // Best score: 13/21
            //Examples.SEQ60, // Best score: 17/34
            //Examples.SEQ64 // Best score: 25/42
        };

        for (String benchmark : benchmarks) {
            String sequence = convertToHP(benchmark);
            runGeneticAlgorithm(sequence);
        }
    }

    private static String convertToHP(String binarySequence) {
        StringBuilder hpSequence = new StringBuilder();
        for (char c : binarySequence.toCharArray()) {
            if (c == '1') {
                hpSequence.append('H');
            } else {
                hpSequence.append('P');
            }
        }
        return hpSequence.toString();
    }

    private static void runGeneticAlgorithm(String SEQUENCE) throws IOException {
    
        // Initialize the population with random solutions
        List<HPModel> population = initializePopulation(SEQUENCE);
        HPModel bestSolution = null;

        // Create a CSV file to log the results of each generation
        try (FileWriter writer = new FileWriter(CSV_FILE)) {
            writer.write("Generation;AverageFitness;BestFitness;BestOverallFitness;HydrophobicContacts;Overlaps\n");

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
                writer.write(String.format(Locale.GERMAN, "%d;%.2f;%.2f;%.2f;%d;%d\n",
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
                for (int i = 0; i < POPULATION_SIZE / 2; i++) { // Da wir zwei Kinder pro Crossover erzeugen, halbieren wir die Schleifenanzahl
                    final List<HPModel> finalPopulation = population;
                    futures.add(executor.submit(() -> {
                        HPModel parent1 = selectParent(finalPopulation);
                        HPModel parent2 = selectParent(finalPopulation);
                        List<HPModel> offspringList = crossover(parent1, parent2, new Random(), SEQUENCE);
                        for (HPModel offspring : offspringList) {
                            mutate(offspring, new Random());
                            synchronized (newGeneration) {
                                newGeneration.add(offspring);
                            }
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

            generateImageForBestSolution(bestSolution, GENERATIONS);
        }
    }
         
    private static void generateImageForBestSolution(HPModel bestSolution, int generation) {
    int height = 500;
    int width = 800;

    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = image.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, width, height);

    int cellSize = 20;
    int offsetX = width / 2;
    int offsetY = height / 2;

    // Draw the HP model
    Map<Integer, AminoAcid> aminoAcids = bestSolution.getAminoAcids();
    Map<String, Integer> positionCount = new HashMap<>();
    for (AminoAcid acid : aminoAcids.values()) {
        String position = acid.getX() + "," + acid.getY();
        positionCount.put(position, positionCount.getOrDefault(position, 0) + 1);
    }

    for (int i = 0; i < aminoAcids.size(); i++) {
        AminoAcid acid = aminoAcids.get(i);
        int x = offsetX + acid.getX() * cellSize;
        int y = offsetY - acid.getY() * cellSize; // Invert y-coordinate

        String position = acid.getX() + "," + acid.getY();
        if (positionCount.get(position) > 1) {
            g2.setColor(Color.ORANGE); // Overlapping amino acids
        } else if (acid.getType() == 'H') {
            g2.setColor(Color.BLACK); // Hydrophobic
        } else {
            g2.setColor(Color.WHITE); // Hydrophilic
            g2.fillOval(x, y, cellSize, cellSize);
            g2.setColor(Color.BLACK); // Border for hydrophilic
            g2.drawOval(x, y, cellSize, cellSize);
        }

        if (acid.getType() == 'H' || positionCount.get(position) > 1) {
            g2.fillOval(x, y, cellSize, cellSize);
        }

        // Draw the index of the amino acid
        g2.setColor(Color.RED);
        g2.drawString(String.valueOf(acid.getId()), x + cellSize / 2 - 4, y + cellSize / 2 + 4);

        if (i > 0) {
            AminoAcid prev = aminoAcids.get(i - 1);
            int prevX = offsetX + prev.getX() * cellSize;
            int prevY = offsetY - prev.getY() * cellSize; // Invert y-coordinate
            g2.setColor(Color.RED);
            g2.drawLine(prevX + cellSize / 2, prevY + cellSize / 2, x + cellSize / 2, y + cellSize / 2);
        }
    }

    // Add text with important information
    g2.setColor(Color.BLACK);
    g2.drawString("Generation: " + generation, 10, 20);
    g2.drawString("Fitness Score: " + bestSolution.calculateFitnessScore(), 10, 40);
    g2.drawString("Energy: " + bestSolution.calculateEnergy(), 10, 60);
    g2.drawString("Overlaps: " + bestSolution.countOverlaps(), 10, 80);

    String folder = "ga_images";
    String filename = "generation_" + generation + ".png";
    File dir = new File(folder);
    if (!dir.exists()) {
        dir.mkdirs();
    }

    try {
        ImageIO.write(image, "png", new File(dir, filename));
    } catch (IOException e) {
        e.printStackTrace();
        System.exit(0);
    }
}

    // Initialize the population with random moves
    private static List<HPModel> initializePopulation(String SEQUENCE) {
        List<HPModel> population = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            String moves = randomMoves(SEQUENCE.length() - 1, random);
            population.add(new HPModel(SEQUENCE, moves));
        }
        return population;
    }

    private static String randomMoves(int length, Random random) {
        char[] moves = new char[length];
        char[] possibleMoves = {'U', 'D', 'L', 'R'};
        char lastMove = ' '; // Initialer Wert, der keine Richtung darstellt
    
        for (int i = 0; i < length; i++) {
            char[] validMoves = getValidMoves(lastMove);
            moves[i] = validMoves[random.nextInt(validMoves.length)];
            lastMove = moves[i];
        }
        return new String(moves);
    }
    
    private static char[] getValidMoves(char lastMove) {
        switch (lastMove) {
            case 'U':
                return new char[] {'U', 'L', 'R'};
            case 'D':
                return new char[] {'D', 'L', 'R'};
            case 'L':
                return new char[] {'U', 'D', 'L'};
            case 'R':
                return new char[] {'U', 'D', 'R'};
            default:
                return new char[] {'U', 'D', 'L', 'R'};
        }
    }

    private static HPModel selectParent(List<HPModel> population) {
        // Berechne die Summe aller Fitness-Scores
        double totalFitness = population.stream().mapToDouble(HPModel::calculateFitnessScore).sum();
    
        // Berechne die prozentualen Anteile der Fitness-Scores
        List<Double> cumulativeProbabilities = new ArrayList<>();
        double cumulativeSum = 0.0;
        for (HPModel model : population) {
            cumulativeSum += model.calculateFitnessScore() / totalFitness;
            cumulativeProbabilities.add(cumulativeSum);
        }
    
        // Erstelle eine Zufallszahl zwischen 0 und 1
        Random random = new Random();
        double randomValue = random.nextDouble();
    
        // Finde den Kandidaten, in dessen Bereich die Zufallszahl fällt
        for (int i = 0; i < cumulativeProbabilities.size(); i++) {
            if (randomValue <= cumulativeProbabilities.get(i)) {
                return population.get(i);
            }
        }
    
        // Falls keine Auswahl getroffen wurde, gib das letzte Element zurück (sollte nicht passieren)
        return population.get(population.size() - 1);
    }

    // Perform crossover between two parents to create a new offspring
    private static List<HPModel> crossover(HPModel parent1, HPModel parent2, Random random, String SEQUENCE) {
        String moves1 = parent1.getMoves();
        String moves2 = parent2.getMoves();
        char[] newMoves1 = new char[moves1.length()];
        char[] newMoves2 = new char[moves1.length()];
    
        // Wähle einen zufälligen Punkt für den Crossover
        int crossoverPoint = random.nextInt(moves1.length());
    
        // Kombiniere die Teile der Elternteile, um zwei neue Kinder zu erzeugen
        for (int i = 0; i < moves1.length(); i++) {
            if (i < crossoverPoint) {
                newMoves1[i] = moves1.charAt(i);
                newMoves2[i] = moves2.charAt(i);
            } else {
                newMoves1[i] = moves2.charAt(i);
                newMoves2[i] = moves1.charAt(i);
            }
        }
    
        // Erzeuge zwei neue HPModel-Objekte mit den kombinierten Zügen
        HPModel child1 = new HPModel(SEQUENCE, new String(newMoves1));
        HPModel child2 = new HPModel(SEQUENCE, new String(newMoves2));
    
        // Rückgabe der beiden Kinder als Liste
        List<HPModel> children = new ArrayList<>();
        children.add(child1);
        children.add(child2);
        return children;
    }

    // Apply mutation to the model's moves
    private static void mutate(HPModel model, Random random) {
        char[] moves = model.getMoves().toCharArray();
    
        // Wähle einen zufälligen Punkt für die Mutation
        int mutationPoint = random.nextInt(moves.length);
    
        // Bestimme die gültigen Bewegungen basierend auf der vorherigen Bewegung
        char lastMove = mutationPoint > 0 ? moves[mutationPoint - 1] : ' ';
        char[] validMoves = getValidMoves(lastMove);
    
        // Wähle eine zufällige gültige Bewegung für die Mutation
        moves[mutationPoint] = validMoves[random.nextInt(validMoves.length)];
    
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

