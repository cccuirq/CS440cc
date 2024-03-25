package src.pas.battleship.agents;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;

// SYSTEM IMPORTS

// JAVA PROJECT IMPORTS
import edu.bu.battleship.agents.Agent;
import edu.bu.battleship.game.Game.GameView;
import edu.bu.battleship.game.ships.Ship.ShipType;
import edu.bu.battleship.game.EnemyBoard;
import edu.bu.battleship.game.EnemyBoard.Outcome;
import edu.bu.battleship.utils.Coordinate;

public class ProbabilisticAgent
        extends Agent {

    public ProbabilisticAgent(String name) {
        super(name);
        System.out.println("[INFO] ProbabilisticAgent.ProbabilisticAgent: constructed agent");
    }

    @Override
    public Coordinate makeMove(final GameView game) {
        // Xi = {aircraft carrier in cell i|Y }∪{battleship in cell i|Y }∪· · ·∪{patrol
        // boat in cell i|Y }
        EnemyBoard.Outcome[][] enemyBoard = game.getEnemyBoardView();
        // java.util.Map<ShipType, java.lang.Integer> enemyShips =
        // game.getEnemyShipTypeToNumRemaining();

        // If there is no square with 'HIT', randomly choose one.
        Integer col = game.getGameConstants().getNumCols();
        Integer row = game.getGameConstants().getNumRows();
        ArrayList<Coordinate> unknown_coor = new ArrayList<>();
        Map<Coordinate, Double> probs = new HashMap<>();
        Boolean hasHIT = false;
        Coordinate res = null;

        for (int i = 0; i < col; i++) {
            for (int j = 0; j < row; j++) {
                EnemyBoard.Outcome outcome = enemyBoard[i][j];
                if (outcome != null) {
                    if (outcome == EnemyBoard.Outcome.HIT) {
                        hasHIT = true;
                        int dirs[][] = new int[][] { { -1, 0 }, { +1, 0 }, { 0, -1 }, { 0, +1 }, { -2, 0 }, { +2, 0 },
                                { 0, -2 }, { 0, +2 }, { -3, 0 }, { +3, 0 }, { 0, -3 }, { 0, +3 }, { -4, 0 }, { +4, 0 },
                                { 0, -4 }, { 0, +4 } };
                        for (int dir[] : dirs) {
                            int new_x = i + dir[0];
                            int new_y = j + dir[1];
                            Coordinate c = new Coordinate(new_x, new_y);
                            if (game.isInBounds(new_x, new_y) && enemyBoard[new_x][new_y] == EnemyBoard.Outcome.UNKNOWN
                                    && !probs.containsKey(c)) {
                                Double prob = getProb(c, game);
                                probs.put(c, prob);
                            }
                        }
                        // break;
                    } else if (outcome == EnemyBoard.Outcome.UNKNOWN) {
                        Coordinate cur = new Coordinate(i, j);
                        // Double prob = getProb(cur, game);
                        // probs.put(cur, prob);
                        unknown_coor.add(cur);
                    }
                }
            }
            // if (hasHIT) {
            // break;
            // }
        }
        if (!hasHIT) {
            Collections.shuffle(unknown_coor);
            return unknown_coor.get(0);
        }

        // Set<ShipType> types = EnumSet.noneOf(ShipType.class);

        Double max = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Coordinate, Double> entry : probs.entrySet()) {
            if (entry.getValue() > max) {
                res = entry.getKey();
            }
        }
        return res;
    }

    private Double getProb(Coordinate c, final GameView game) {
        EnemyBoard.Outcome[][] enemyBoard = game.getEnemyBoardView();
        Double totalCases = 0d;
        Double hitCases = 0d;

        for (ShipType type : ShipType.values()) {
            java.util.Map<ShipType, java.lang.Integer> enemyShips = game.getEnemyShipTypeToNumRemaining();
            if (enemyShips.get(type) == 0) {
                continue;
            }
            int length = shipLength(type);
            for (int direction = 0; direction < 2; direction++) {
                for (int diff = 0; diff < length; diff++) {
                    int start_x = c.getXCoordinate();
                    int start_y = c.getYCoordinate();
                    int end_x = start_x;
                    int end_y = start_y;
                    if (direction == 0) {
                        start_x = start_x - diff;
                        end_x = start_x + length - 1;
                    } else {
                        start_y = start_y - diff;
                        end_y = start_y + length - 1;
                    }

                    if (game.isInBounds(start_x, start_y) && game.isInBounds(end_x, end_y)) {
                        Boolean valid = true;
                        Double n_HIT = 0d;

                        for (int i = 0; i < length; i++) {
                            int x = start_x;
                            int y = start_y;
                            if (direction == 0) {
                                x = x + i;
                            } else {
                                y = y + i;
                            }
                            EnemyBoard.Outcome outcome = enemyBoard[x][y];
                            if (outcome != null) {
                                if (outcome == EnemyBoard.Outcome.MISS || outcome == EnemyBoard.Outcome.SUNK) {
                                    valid = false;
                                    break;
                                } else if (outcome == EnemyBoard.Outcome.HIT) {
                                    n_HIT += 1d;
                                }
                            }
                        }
                        if (valid) {
                            totalCases += 1d;
                            hitCases += n_HIT; // if there is more HIT in a ship, higher probability
                        }
                    }
                }
            }
        }

        Double res = 0d;
        if (totalCases > 0) {
            res = hitCases / totalCases;
        }

        return res;
    }

    private int shipLength(ShipType type) {
        int length = 0;
        if (type == ShipType.PATROL_BOAT) {
            length = 2;
        } else if (type == ShipType.SUBMARINE || type == ShipType.DESTROYER) {
            length = 3;
        } else if (type == ShipType.BATTLESHIP) {
            length = 4;
        } else if (type == ShipType.AIRCRAFT_CARRIER) {
            length = 5;
        }
        return length;
    }

    @Override
    public void afterGameEnds(final GameView game) {
    }

}
