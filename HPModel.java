import java.util.HashMap;
import java.util.Map;

class AminoAcid {
    private final int id;
    private final char type;
    private final int x;
    private final int y;

    public AminoAcid(int id, char type, int x, int y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public char getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

public class HPModel {
    private final String sequence;
    private String moves;
    private final Map<Integer, AminoAcid> aminoAcids;

    public HPModel(String sequence, String moves) {
        this.sequence = sequence;
        this.moves = moves;
        this.aminoAcids = new HashMap<>();
        calculatePositions();
    }

    private void calculatePositions() {
        int x = 0, y = 0;
        aminoAcids.put(0, new AminoAcid(0, sequence.charAt(0), x, y));

        for (int i = 1; i < sequence.length(); i++) {
            char move = moves.charAt(i - 1);
            switch (move) {
                case 'R' -> x++;
                case 'L' -> x--;
                case 'U' -> y++;
                case 'D' -> y--;
            }
            aminoAcids.put(i, new AminoAcid(i, sequence.charAt(i), x, y));
        }
    }

    public int calculateEnergy() {
        int energy = 0;
        for (int i = 0; i < sequence.length(); i++) {
            AminoAcid acid1 = aminoAcids.get(i);
            for (int j = i + 1; j < sequence.length(); j++) {
                AminoAcid acid2 = aminoAcids.get(j);
                if (acid1.getType() == 'H' && acid2.getType() == 'H' &&
                    Math.abs(acid1.getX() - acid2.getX()) + Math.abs(acid1.getY() - acid2.getY()) == 1) {
                    // Überprüfen, ob sie Sequenznachbarn sind
                    if (Math.abs(acid1.getId() - acid2.getId()) != 1) {
                        energy--;
                    }
                }
            }
        }
        return energy;
    }

    public int countOverlaps() {
        Map<String, Integer> positionCount = new HashMap<>();
        int overlaps = 0;
    
        // Zähle die Anzahl der Aminosäuren an jeder Position
        for (AminoAcid acid : aminoAcids.values()) {
            String position = acid.getX() + "," + acid.getY();
            positionCount.put(position, positionCount.getOrDefault(position, 0) + 1); //?
        }
    
        // Berechne die Anzahl der Überlappungen basierend auf den Paaren
        for (int count : positionCount.values()) {
            if (count > 1) {
                overlaps += (count * (count - 1)) / 2; // Anzahl der Paare bei Überlappung
            }
        }
    
        return overlaps;
    }

    public double calculateFitnessScore() {
        int energy = calculateEnergy();
        int overlaps = countOverlaps();
        return Math.abs(energy) * Math.pow(0.1, overlaps);
    }

    public String getMoves() {
        return moves;
    }

    public void setMoves(String moves) {
        this.moves = moves;
        calculatePositions();
    }

    public void printGrid() {
        // Bestimme die minimalen und maximalen x- und y-Werte
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (AminoAcid acid : aminoAcids.values()) {
            if (acid.getX() < minX) minX = acid.getX();
            if (acid.getX() > maxX) maxX = acid.getX();
            if (acid.getY() < minY) minY = acid.getY();
            if (acid.getY() > maxY) maxY = acid.getY();
        }

        // Initialisiere das Grid
        int width = (maxX - minX + 1) * 2 - 1;
        int height = (maxY - minY + 1) * 2 - 1;
        String[][] grid = new String[height][width];

        // Fülle das Grid mit Leerzeichen
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                grid[i][j] = "   ";
            }
        }

        // Fülle das Grid mit Aminosäuren und IDs
        for (int i = 0; i < sequence.length(); i++) {
            AminoAcid acid = aminoAcids.get(i);
            int x = (acid.getX() - minX) * 2;
            int y = (acid.getY() - minY) * 2;
            grid[y][x] = String.format("%c%02d", acid.getType(), acid.getId());

            // Zeichne Verbindung zum Vorgänger
            if (i > 0) {
                AminoAcid prevAcid = aminoAcids.get(i - 1);
                int prevX = (prevAcid.getX() - minX) * 2;
                int prevY = (prevAcid.getY() - minY) * 2;

                if (x == prevX) {
                    // Vertikale Verbindung
                    int startY = Math.min(y, prevY) + 1;
                    int endY = Math.max(y, prevY);
                    for (int j = startY; j < endY; j++) {
                        grid[j][x] = " | ";
                    }
                } else if (y == prevY) {
                    // Horizontale Verbindung
                    int startX = Math.min(x, prevX) + 1;
                    int endX = Math.max(x, prevX);
                    for (int j = startX; j < endX; j++) {
                        grid[y][j] = "---";
                    }
                }
            }
        }

        // Drucke das Grid
        for (int i = grid.length - 1; i >= 0; i--) {  // Von oben nach unten drucken
            for (String item : grid[i]) {
                System.out.print(item + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        //String sequence = "HPHHPPPH";
        //String moves = "LDRDLLDDRDLLUULURUR";

        String sequence = "HPHPPHHPHPPHPHHPPHPH";
        String moves = "LDRDLLDDRDLLUULURUR";

        HPModel model = new HPModel(sequence, moves);
        System.out.println("Minimale Energie: " + model.calculateEnergy());
        System.out.println("Anzahl der Überlappungen: " + model.countOverlaps());
        System.out.println("Fitness-Score: " + model.calculateFitnessScore());
        
    
        model.printGrid();
    }
}