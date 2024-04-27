package src.pas.tetris.agents;

// SYSTEM IMPORTS
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

import edu.bu.tetris.utils.Coordinate;
// JAVA PROJECT IMPORTS
import edu.bu.tetris.agents.QAgent;
import edu.bu.tetris.agents.TrainerAgent.GameCounter;
import edu.bu.tetris.game.Board;
import edu.bu.tetris.game.Game.GameView;
import edu.bu.tetris.game.minos.Mino;
import edu.bu.tetris.linalg.Matrix;
import edu.bu.tetris.nn.Model;
import edu.bu.tetris.nn.LossFunction;
import edu.bu.tetris.nn.Optimizer;
import edu.bu.tetris.nn.models.Sequential;
import edu.bu.tetris.nn.layers.Dense; // fully connected layer
import edu.bu.tetris.nn.layers.ReLU; // some activations (below too)
import edu.bu.tetris.nn.layers.Tanh;
import edu.bu.tetris.nn.layers.Sigmoid;
import edu.bu.tetris.training.data.Dataset;
import edu.bu.tetris.utils.Pair;

public class TetrisQAgent
        extends QAgent {

    public static final double EXPLORATION_PROB = 0.05;

    private Random random;
    private Map<Mino, Integer> visitCounts;

    public TetrisQAgent(String name) {
        super(name);
        this.random = new Random(12345); // optional to have a seed
        this.visitCounts = new HashMap<>();
    }

    public Random getRandom() {
        return this.random;
    }

    @Override
    public Model initQFunction() {
        // build a single-hidden-layer feedforward network
        // this example will create a 3-layer neural network (1 hidden layer)
        // in this example, the input to the neural network is the
        // image of the board unrolled into a giant vector
        final int numPixelsInImage = 12;
        final int hiddenDim = 2 * numPixelsInImage;
        final int outDim = 1;

        Sequential qFunction = new Sequential();
        qFunction.add(new Dense(numPixelsInImage, hiddenDim));
        qFunction.add(new Tanh());
        qFunction.add(new Dense(hiddenDim, outDim));

        return qFunction;
    }

    /**
     * This function is for you to figure out what your features
     * are. This should end up being a single row-vector, and the
     * dimensions should be what your qfunction is expecting.
     * One thing we can do is get the grayscale image
     * where squares in the image are 0.0 if unoccupied, 0.5 if
     * there is a "background" square (i.e. that square is occupied
     * but it is not the current piece being placed), and 1.0 for
     * any squares that the current piece is being considered for.
     * 
     * We can then flatten this image to get a row-vector, but we
     * can do more than this! Try to be creative: how can you measure the
     * "state" of the game without relying on the pixels? If you were given
     * a tetris game midway through play, what properties would you look for?
     */
    @Override
    public Matrix getQFunctionInput(final GameView game,
            final Mino potentialAction) {
        Matrix flattenedImage = null;
        Matrix gameMatrix = null;

        Matrix result = Matrix.zeros(1, 12);

        // collect data
        int num_holes_inside = 0; // empty spaces that have blocks above them
        int maxHeight = 0;
        int maxHeight_aft = 0;
        int maxHeight_bf = 0;
        int diff = 0;
        int holes = 0;
        int bumpiness = 0; // the variance between the heights of adjacent columns, higher the variance,
                           // less opportunity to cancel out lines
        int[] minoTypes = new int[] { 0, 0, 0, 0, 0, 0, 0 };
        Board board = game.getBoard();

        try {
            flattenedImage = game.getGrayscaleImage(potentialAction).flatten();
            // get the grayscale image of the game board
            gameMatrix = game.getGrayscaleImage(potentialAction);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        // mino type set up
        minoTypes[potentialAction.getType().ordinal()] = 1;

        // calculation
        int previousHeight = -1;
        for (int y = 0; y < gameMatrix.getShape().getNumCols(); y++) {
            int colHeight = 0;
            boolean found = false;
            boolean goingto = false;// maximum height after I place current piece
            int colHoles = 0;
            int currentheight = 0;
            for (int x = 0; x < gameMatrix.getShape().getNumRows(); x++) {
                Coordinate c = new Coordinate(x, y);
                if (board.isInBounds(c) && board.isCoordinateOccupied(c)) {
                    found = true;
                    colHeight = Board.NUM_ROWS - x;
                    if (maxHeight < colHeight) {
                        maxHeight = colHeight;
                    }
                } else if (found) {
                    colHoles++;
                }

                if (gameMatrix.get(y, x) == 0.5) {
                    // sets maxHeight before
                    if (goingto == false) {
                        maxHeight_bf = y;
                        goingto = true;
                    }
                }
                if (gameMatrix.get(y, x) == 1.0) {
                    // sets maxHeight after
                    if (goingto == false) {
                        maxHeight_aft = y;
                        goingto = true;
                    }
                }

                if (gameMatrix.get(y, x) == 0.0) {
                    // iterates numHoles when coordinate above is filled
                    if (y > 0 && (gameMatrix.get(y - 1, x) == 1.0 || gameMatrix.get(y - 1, x) == 0.5)) {
                        num_holes_inside += 1;
                    }
                }

                // calculate bumpiness
                if (gameMatrix.get(x, y) > 0) {
                    currentheight = Board.NUM_ROWS - x;
                    break;
                }
                if (previousHeight != -1) {
                    bumpiness += Math.abs(currentheight - previousHeight);
                }
                previousHeight = currentheight;
            }
            holes += colHoles;
        }
        int diff2 = maxHeight_bf - maxHeight_aft;
        if (diff2 <= 0) {
            diff = 0;
        } else {
            diff = diff2;
        }
        result.set(0, 0, maxHeight);
        result.set(0, 1, holes);
        result.set(0, 2, num_holes_inside);
        result.set(0, 3, diff);
        for (int i = 0; i < minoTypes.length; i++) {
            result.set(0, 4 + i, minoTypes[i]);
        }
        result.set(0, 11, bumpiness);
        // System.out.println(result);
        return result;
    }

    /**
     * This method is used to decide if we should follow our current policy
     * (i.e. our q-function), or if we should ignore it and take a random action
     * (i.e. explore).
     *
     * Remember, as the q-function learns, it will start to predict the same "good"
     * actions
     * over and over again. This can prevent us from discovering new, potentially
     * even
     * better states, which we want to do! So, sometimes we should ignore our policy
     * and explore to gain novel experiences.
     *
     * The current implementation chooses to ignore the current policy around 5% of
     * the time.
     * While this strategy is easy to implement, it often doesn't perform well and
     * is
     * really sensitive to the EXPLORATION_PROB. I would recommend devising your own
     * strategy here.
     */
    @Override
    public boolean shouldExplore(final GameView game,
            final GameCounter gameCounter) {
        long t = gameCounter.getCurrentMoveIdx();

        return this.getRandom().nextDouble() <= (1 / (5 + t));
        // return this.getRandom().nextDouble() <= EXPLORATION_PROB;
    }

    /**
     * This method is a counterpart to the "shouldExplore" method. Whenever we
     * decide
     * that we should ignore our policy, we now have to actually choose an action.
     *
     * You should come up with a way of choosing an action so that the model gets
     * to experience something new. The current implemention just chooses a random
     * option, which in practice doesn't work as well as a more guided strategy.
     * I would recommend devising your own strategy here.
     */
    @Override
    public Mino getExplorationMove(final GameView game) {
        List<Mino> possibleMoves = game.getFinalMinoPositions();
        Mino leastVisited = null;
        int minV = Integer.MAX_VALUE;

        // Iterate over all possible moves and find the one with the least visits
        for (Mino mino : possibleMoves) {
            int visits = visitCounts.getOrDefault(mino, 0);
            if (visits < minV) {
                minV = visits;
                leastVisited = mino;
            }
        }
        // Update the visit count for the selected Mino
        visitCounts.put(leastVisited, minV + 1);

        return leastVisited;
    }

    /**
     * This method is called by the TrainerAgent after we have played enough
     * training games.
     * In between the training section and the evaluation section of a phase, we
     * need to use
     * the exprience we've collected (from the training games) to improve the
     * q-function.
     *
     * You don't really need to change this method unless you want to. All that
     * happens
     * is that we will use the experiences currently stored in the replay buffer to
     * update
     * our model. Updates (i.e. gradient descent updates) will be applied per
     * minibatch
     * (i.e. a subset of the entire dataset) rather than in a vanilla gradient
     * descent manner
     * (i.e. all at once)...this often works better and is an active area of
     * research.
     * 
     * Each pass through the data is called an epoch, and we will perform
     * "numUpdates" amount
     * of epochs in between the training and eval sections of each phase.
     */
    @Override
    public void trainQFunction(Dataset dataset,
            LossFunction lossFunction,
            Optimizer optimizer,
            long numUpdates) {
        for (int epochIdx = 0; epochIdx < numUpdates; ++epochIdx) {
            dataset.shuffle();
            Iterator<Pair<Matrix, Matrix>> batchIterator = dataset.iterator();

            while (batchIterator.hasNext()) {
                Pair<Matrix, Matrix> batch = batchIterator.next();

                try {
                    Matrix YHat = this.getQFunction().forward(batch.getFirst());

                    optimizer.reset();
                    this.getQFunction().backwards(batch.getFirst(),
                            lossFunction.backwards(YHat, batch.getSecond()));
                    optimizer.step();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * This method is where you will devise your own reward signal. Remember, the
     * larger
     * the number, the more "pleasurable" it is to the model, and the smaller the
     * number,
     * the more "painful" to the model.
     *
     * This is where you get to tell the model how "good" or "bad" the game is.
     * Since you earn points in this game, the reward should probably be influenced
     * by the
     * points, however this is not all. In fact, just using the points earned this
     * turn
     * is a **terrible** reward function, because earning points is hard!!
     *
     * I would recommend you to consider other ways of measuring "good"ness and
     * "bad"ness
     * of the game. For instance, the higher the stack of minos gets....generally
     * the worse
     * (unless you have a long hole waiting for an I-block). When you design a
     * reward
     * signal that is less sparse, you should see your model optimize this reward
     * over time.
     */
    @Override
    public double getReward(final GameView game) {
        Board board = game.getBoard();
        int numemptyb = 0;
        Coordinate highest = null;
        Boolean beginempty = false;
        double reward = 0.0;

        for (int y = 0; y < Board.NUM_COLS; y++) {
            beginempty = false;
            for (int x = 0; x < Board.NUM_ROWS; x++) {
                Coordinate c = new Coordinate(x, y);
                if (board.isInBounds(c) && board.isCoordinateOccupied(c) && !beginempty) {
                    // get the highest coordinate position
                    highest = c;
                    beginempty = true;
                } else if (board.isInBounds(c) && !(board.isCoordinateOccupied(x, y)) && beginempty) {
                    // number of occupied below highest
                    numemptyb += 1;
                }
            }
        }

        int complete = 0;
        int almost = 0;
        int total = 0;
        for (int row = 0; row < Board.NUM_ROWS; row++) {
            // boolean completeLine = true;
            Double squares = 0d;
            for (int col = 0; col < Board.NUM_COLS; col++) {
                Coordinate c = new Coordinate(row, col);
                if (board.isInBounds(c) && !board.isCoordinateOccupied(c)) {
                    // completeLine = false;
                } else if (board.isInBounds(c) && board.isCoordinateOccupied(c)) {
                    squares++;
                    total++;
                }
            }
            if (squares / Board.NUM_COLS == 1.0) {
                complete++;
            } else if (squares / Board.NUM_COLS >= 0.5) {
                almost++;
            }
        }

        if (highest != null) {
            double highestY = (Board.NUM_ROWS - highest.getYCoordinate());// higher the highest, smaller the highestY
            // more height should minus more point
            reward = (5 * total / highestY) - numemptyb + 7 * almost + 10 * complete + 2 * game.getScoreThisTurn();
            ;// reward consider height and score
             // System.out.println("Reward value: " + reward + "num empty" + numemptyb);
        }
        return reward;
    }

}
