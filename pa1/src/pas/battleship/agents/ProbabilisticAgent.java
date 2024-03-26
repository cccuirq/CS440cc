package src.pas.battleship.agents;


// SYSTEM IMPORTS
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.Hashtable;
import java.util.List;

// JAVA PROJECT IMPORTS
import edu.bu.battleship.agents.Agent;
import edu.bu.battleship.game.Game.GameView;
import edu.bu.battleship.game.EnemyBoard.Outcome;
import edu.bu.battleship.utils.Coordinate;


public class ProbabilisticAgent
    extends Agent
{

    public ProbabilisticAgent(String name)
    {
        super(name);
        System.out.println("[INFO] ProbabilisticAgent.ProbabilisticAgent: constructed agent");
    }

    @Override
    public Coordinate makeMove(final GameView game)
    {
        // Ramdomly pick any position before we successfully hit ones
        Random random = new Random();

        // picks random value that is in bounds
        int col = random.nextInt(game.getGameConstants().getNumCols());
        int row = random.nextInt(game.getGameConstants().getNumRows());

        //generates coordinate to fire at
        Coordinate fire = new Coordinate (row, col);

        // ensures no duplicate positions are fired upon for random shot picking
        while(!game.getEnemyBoardView()[fire.getXCoordinate()][fire.getYCoordinate()].toString().equals("UNKNOWN")) {
            col = random.nextInt(game.getGameConstants().getNumCols());
            row = random.nextInt(game.getGameConstants().getNumRows());
            fire = new Coordinate (row, col);
        }
        
        if(!game.getEnemyBoardView()[fire.getXCoordinate()][fire.getYCoordinate()].toString().equals("HIT")){
             // shot to be fired
            return fire;
        }

        // create probability matrix for us to pick the best next shot
        double[][] probM = this.probabilityMatrix(game);

        // Visualize current probability matrix
        ArrayList<Integer[]> hitAdj = this.visualize(game, probM); 
        System.out.print("Coordinates Adj to hits: ");   
        for (Integer[] x : hitAdj) {
            System.out.print("(");
            int z = 0;
            for (int y : x) {
                if (z % 2 == 0) {
                    System.out.print(y + ", ");
                }
                else {
                    System.out.print(y);
                }
                z +=1 ;
            }
            System.out.print(") ");
        }
        System.out.println();

        // Find the next postion to shot given probability
        Coordinate yesss = this.next(game, probM, hitAdj);
        System.out.println("Highest probability shot: " + yesss.toString());

        return yesss;
    }
    
    public double[][] probabilityMatrix(final GameView game){
        //create a matrix for each individual cordinate probability
        double[][] probM = new double[game.getGameConstants().getNumRows()][game.getGameConstants().getNumCols()];

        for(int y = 0; y < game.getGameConstants().getNumCols(); y++) {
            for(int x = 0; x < game.getGameConstants().getNumRows(); x++) {
                if (game.getEnemyBoardView()[x][y].toString().equals("HIT")) {
                    // set the value of a hit coordinate to 1.0
                    probM[x][y] = 1.0;

                    // set the hitAdjacent coordinates, ensuring it is in bounds and not to overwrite any hits, misses, or sinks
                    if (game.isInBounds(x+1, y) && probM[x+1] [y] != -1.0 && probM[x+1][y] != 1.0 && probM[x+1][y] != -2.0) {
                        probM[x+1][y] = 0.85;
                        //System.out.println(probMat[x+1][y]);
                    }
                    if (game.isInBounds(x-1, y) && probM[x-1][y] != -1.0 && probM[x-1][y] != 1.0 && probM[x-1][y] != -2.0) {
                        probM[x-1][y] = 0.85;
                        //System.out.println(probMat[x-1][y]);
                    }
                    if (game.isInBounds(x, y+1) && probM[x][y+1] != -1.0 && probM[x][y+1] != 1.0 && probM[x][y+1] != -2.0) {
                        probM[x][y+1] = 0.85;
                        //System.out.println(probMat[x][y+1]);
                    }
                    if (game.isInBounds(x, y-1) && probM[x][y-1] != -1.0 && probM[x][y-1] != 1.0 && probM[x][y-1] != -2.0) {
                        probM[x][y-1] = 0.85;
                        //System.out.println(probMat[x][y-1]);
                    }
                }
                else if (game.getEnemyBoardView()[x][y].toString().equals("MISS")) {
                    // set the value of a miss to -1.0
                    probM[x][y] = -1.0;
                }
                else if (game.getEnemyBoardView()[x][y].toString().equals("SUNK")) {
                    // set the value of a sink to -2.0
                    probM[x][y] = -2.0;
                }
                // if the value has not been previously changed by the hitAdjacent logic then update to unknown value for that coordinate
                else if (probM[x][y] == 0.0) {
                    double val = 0.0;
                    ArrayList<Integer> perm = coordPermutations(game, new Coordinate(x, y));

                    for(int z = 0; z < perm.size(); z++) {
                        if(z==0)
                        {
                            val += (double)perm.get(z) * (1.0 / (double)(game.getGameConstants().getNumRows() * game.getGameConstants().getNumCols()));
                        }
                        else if(z==1)
                        {
                            val += (double)perm.get(z) * (2.0 / (double)(game.getGameConstants().getNumRows() * game.getGameConstants().getNumCols()));
                        }
                        else if(z==2)
                        {
                            val += (double)perm.get(z) * (3.0 / (double)(game.getGameConstants().getNumRows() * game.getGameConstants().getNumCols()));
                        }
                        else if(z==3)
                        {
                            val += (double)perm.get(z) * (4.0 / (double)(game.getGameConstants().getNumRows() * game.getGameConstants().getNumCols()));
                        }
                    }
                    probM[x][y] = val;
                }
            }
        }
        // return the completed probability matrix
        return probM;
    }

    public Coordinate next(final GameView game, double[][] probMat, ArrayList<Integer[]> adjacents) {
        // init the max value that has been see so far
        double bestProb = -1.0;
        Coordinate bestShot = new Coordinate(100, 100);

        // if there are no hitAdj coords then dont do shit
        if (adjacents.size() != 0) {
            // will pick the proability with the highest chance of being a hit
            for (Integer[] x : adjacents) {
                if (probMat[x[0]][x[1]] > bestProb) {
                    bestProb = probMat[x[0]][x[1]];
                    bestShot = new Coordinate(x[0], x[1]);
                }
            }
        }
        else {
            // pick the highest probability coordinate when no adjacents

        }
        return bestShot;
    }

    public ArrayList<Integer[]> visualize(final GameView game,  double[][] probMatrix) {
        ArrayList<Integer[]> close = new ArrayList<>();

        // nested for loop that will go over the probabilities as they are generated from probabilityMatrix for ease of viewing (can switch out to exact prob values)
        for(int y = 0; y < probMatrix.length; y++) {
            for(int x = 0; x < probMatrix[0].length; x++) {
                // a probability of 1.0 in a coordinate denotes a hit 
                if (probMatrix[x][y] == 1.0) {
                    //System.out.print("|  " + "HIT" + "  |");
                    System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability of -1.0 in a coordinate denotes a miss
                else if (probMatrix[x][y] == -1.0) {
                    //System.out.print("| " + "MISS " + " |");
                    System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability of -2.0 in a coordinate denotes a sink
                else if (probMatrix[x][y] == -2.0) {
                    //System.out.print("| " + "SUNK " + " |");
                    System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability between 0.5 and 1.0 in a coordinate denotes a hit adjacent
                else if (probMatrix[x][y] > 0.8 && probMatrix[x][y] < 1.0) {
                    //System.out.print("| " + "**** " + " |");
                    System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                    close.add(new Integer[] {x, y});
                }
                // otherwise we have not shot at that coordinate
                else {
                    //System.out.print("| " + "UNKW " + " |");
                    System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
            }
            System.out.println();
        }
        // returns the arraylist of values that are hit adjacent
        return close;
    }

    @Override
    public void afterGameEnds(final GameView game) {}

}
