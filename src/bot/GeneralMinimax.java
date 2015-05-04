/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.*;
import java.util.stream.Collectors;
import map.Map;
import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.GeneralMove;
import move.Move;
import move.PlaceArmiesMove;

/**
 *
 * @author Stefan
 */
public class GeneralMinimax {

    private static final Random random = new Random();

    private java.util.Map<String, String> opponent = new HashMap<>();
    public int iterations = 0;

    public GeneralMinimax(BotState state) {
        opponent.put(state.getMyPlayerName(), state.getOpponentPlayerName());
        opponent.put(state.getOpponentPlayerName(), state.getMyPlayerName());
    }

    public BestMove minimax(GameState state, int depth) {
        iterations++;
        if (depth == 0) {
            return new BestMove(evaluate(state), null, null);
        }
        GeneralMove[] myMoves = getMoves(state, state.getMyPlayerName());
        GeneralMove[] opMoves = getMoves(state, state.getOpponentPlayerName());
        //System.err.println("My moves: "+ myMoves.length);
        //System.err.println("Op moves: "+ myMoves.length);
        if (opMoves.length * myMoves.length > 220) {
            depth = 1;
        }
        BestMove[][] scores = new BestMove[myMoves.length][opMoves.length];
        for (int i = 0; i < myMoves.length; i++) {
            for (int j = 0; j < opMoves.length; j++) {
                GameState next = nextState(state, myMoves[i], opMoves[j]);
                BestMove score = minimax(next, depth - 1);
                score.opDeployments = next.opMoves;
                scores[i][j] = score;
            }
        }
        return computeScore(scores, myMoves, opMoves);
    }

    private BestMove computeScore(BestMove[][] scores, GeneralMove[] myMoves, GeneralMove[] opMoves) {
        double[] opScores = new double[scores[0].length];
        for (int i = 0; i < opScores.length; i++) {
            double min = Double.POSITIVE_INFINITY;
            for (int j = 0; j < scores.length; j++) {
                if (scores[j][i].score < min) {
                    min = scores[j][i].score;
                }
            }
            opScores[i] = min;
        }
        double[] opProb = computeProbabilities(opScores, -1);
        double[] myScores = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            double sum = 0;
            for (int j = 0; j < scores[i].length; j++) {
                sum += scores[i][j].score * opProb[j];
            }
            myScores[i] = sum;
        }
        System.err.println("scores:" + Arrays.deepToString(scores));
        System.err.println("opProb:" + Arrays.toString(opProb));
        System.err.println("myScores: " + Arrays.toString(myScores));
        double[] myProb = computeProbabilities(myScores, 1);
        int move = choseBestMove(myProb, 1);
        BestMove best = new BestMove(myScores[move], myMoves[move], null);
        if (opProb.length != 0)
            best.opMove = opMoves[choseBestMove(opProb, -1)];
        return best;

//        double sum = 0;
//        for (int i = 0; i < myScores.length; i++) {
//            sum += myScores[i] * myProb[i];
//        }
//        int chosen = choseMove(myProb);
//        return new BestMove(sum, myMoves[chosen]);
    }

    private double[] computeProbabilities(double[] scores, int sign) {
        double min = Double.POSITIVE_INFINITY;
        for (double s : scores) {
            if (s * sign < min) {
                min = s * sign;
            }
        }
        double sum = 0;
        for (int i = 0; i < scores.length; i++) {
            sum += scores[i] * sign - min;
        }
        double[] prob = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            prob[i] = (scores[i] * sign - min) / sum;
        }
        return prob;
    }

    private int choseRandomMove(double[] prob) {
        double r = random.nextDouble();
        double sum = 0;
        for (int i = 0; i < prob.length; i++) {
            if (r < sum + prob[i]) {
                return i;
            }
            sum += prob[i];
        }
        return prob.length - 1;
    }

    private int choseBestMove(double[] prob, int sign) {
        double max = Double.NEGATIVE_INFINITY;
        int move = 0;
        for (int i = 0; i < prob.length; i++) {
            if (prob[i] * sign > max) {
                max = prob[i] * sign;
                move = i;
            }
        }
        return move;
    }

    public GeneralMove[] getMoves(GameState state, String player) {
        List<GeneralMove> moves = new ArrayList<>();
        Set<SuperRegion> superRegions = new HashSet<>();

        state.getVisibleMap().getRegions().stream().filter((r)
                -> (r.getPlayerName().equals(player))).forEach((r) -> {
                    superRegions.add(r.getSuperRegion());
                });
        int armies;
        if (state.getMyPlayerName().equals(player)) {
            armies = state.getArmiesPerTurn();
        } else {
            armies = state.getOpArmiesPerTurn();
        }
        final int d = armies / 10 + 1;
        superRegions.stream().forEach(sr -> {
            for (int i = 1; i <= armies; i += d) {
                moves.add(new GeneralMove(sr, i));
            }
        });
        return moves.toArray(new GeneralMove[0]);
    }

    public double evaluate(GameState state) {
        double score = state.getVisibleMap().superRegions.stream()
                .filter(superRegion -> superRegion.getSubRegions().size() != 0)
                .mapToDouble(sr -> {
                    long myRegions = sr.getSubRegions().stream()
                    .filter(r -> r.getPlayerName().equals(state.getMyPlayerName()))
                    .count();
                    long opRegions = sr.getSubRegions().stream()
                    .filter(r -> r.getPlayerName().equals(state.getOpponentPlayerName()))
                    .count();
                    //System.err.println("SuperRegion: " + sr.getId() + " " + sr.getSubRegions().size());
                    return Math.pow(2, myRegions * 1. / sr.getSubRegions().size()) * sr.getArmiesReward()
                    - Math.pow(2, opRegions * 1. / sr.getSubRegions().size()) * sr.getArmiesReward();
                }).sum();

        score += state.getVisibleMap().regions.stream().mapToInt(r -> {
            if (!opponent.containsKey(r.getPlayerName())) {
                return 0;
            }
            String op = opponent.get(r.getPlayerName());
            int s = r.getArmies() - r.getNeighbors().stream().filter(n -> n.getPlayerName().equals(opponent.get(op)))
                    .mapToInt(n -> n.getArmies()).sum();
            if (r.getPlayerName().equals(state.getMyPlayerName())) return s;
            return -s;
        }).sum();
        return score;
    }

    /**
     * Returns the expected outcome of deploying the armies defined in
     * {@code myMove} and {@code opMove}. The attacks performed by each player
     * is determined using a greedy strategy.
     *
     * @param state The current state of the game
     * @param myMove The deployment of the bot's troops
     * @param opMove The deployment if the opponent's troops
     * @return The next state
     */
    public GameState nextState(GameState state, GeneralMove myMove, GeneralMove opMove) {
        GameState next = state.clone();
        GameState myState = state.clone();
        GameState opState = state.clone();
        List<Move> opMoves = Heuristics.metaHeuristic(
                opState.getOpponentPlayerName(),
                opMove.getNumber(),
                opState.getVisibleMap().getRegions(),
                opState.getRound(),
                myMove);
        makeDeployMoves(myState.getVisibleMap(), opMoves);
        List<Move> myMoves = Heuristics.metaHeuristic(
                myState.getMyPlayerName(),
                myMove.getNumber(),
                myState.getVisibleMap().getRegions(),
                myState.getRound(),
                myMove);


        makeDeployMoves(next.getVisibleMap(), myMoves);
        makeDeployMoves(next.getVisibleMap(), opMoves);
        Iterator<AttackTransferMove> myIt = myMoves.stream().filter(m -> m instanceof AttackTransferMove)
                .map(m -> (AttackTransferMove) m)
                .collect(Collectors.toList()).iterator();
        Iterator<AttackTransferMove> opIt = opMoves.stream().filter(m -> m instanceof AttackTransferMove)
                .map(m -> (AttackTransferMove) m)
                .collect(Collectors.toList()).iterator();
        while (myIt.hasNext() && opIt.hasNext()) {
            executeAttackMoves(next.getVisibleMap(), myIt.next(), opIt.next());
        }
        while (myIt.hasNext()) {
            executeAttackMove(next.getVisibleMap(), myIt.next());
        }
        while (opIt.hasNext()) {
            executeAttackMove(next.getVisibleMap(), opIt.next());
        }
        next.opMoves = opMoves;

        return next;
    }

    static public void makeDeployMoves(Map map, List<Move> moves) {
        moves.stream().filter(m -> m instanceof PlaceArmiesMove).forEach(m -> {
            PlaceArmiesMove move = (PlaceArmiesMove) m;
            Region r = map.getRegion(move.getRegion().getId());
            r.setArmies(r.getArmies() + move.getArmies());
            r.setMoveableArmies(r.getMoveableArmies() + move.getArmies());
        });
    }

    private void executeAttackMoves(Map map, AttackTransferMove m1, AttackTransferMove m2) {
        if (random.nextBoolean()) {
            executeAttackMove(map, m1);
            executeAttackMove(map, m2);
        } else {
            executeAttackMove(map, m2);
            executeAttackMove(map, m1);
        }
    }

    private void executeAttackMove(Map map, AttackTransferMove move) {
        Region from = map.getRegion(move.getFromRegion().getId());
        Region to = map.getRegion(move.getToRegion().getId());
        if (from.getMoveableArmies() < move.getArmies()) {
            return;
        }
        if (from.getPlayerName().equals(to.getPlayerName())) {
            from.setArmies(from.getArmies() - move.getArmies());
            from.setMoveableArmies(from.getMoveableArmies() - move.getArmies());
            to.setArmies(to.getArmies() + move.getArmies());
        } else {
            int attacking = Math.max(0, move.getArmies() - move.mostProbableAttackingCasualties());
            int defending = Math.max(0, to.getArmies() - move.mostProbableDefendingCasualties());
            if (defending == 0 && attacking > 0) {
                from.setArmies(from.getArmies() - move.getArmies());
                from.setMoveableArmies(from.getMoveableArmies() - move.getArmies());
                to.changeSide(from.getPlayerName(), attacking);
            } else {
                from.setArmies(from.getArmies() - move.getArmies() + attacking);
                from.setMoveableArmies(from.getMoveableArmies() - move.getArmies());
                to.setArmies(defending);
                to.setMoveableArmies(Math.min(to.getMoveableArmies(), to.getArmies() - 1));
            }
        }
    }

    public static class BestMove {

        public double score;
        public GeneralMove move;
        public GeneralMove opMove;
        public List<Move> opDeployments;

        public BestMove(double score, GeneralMove move, GeneralMove opMove) {
            this.score = score;
            this.move = move;
            this.opMove = opMove;
        }

        @Override
        public String toString() {
            return "score: " + score + " move: " + move;
        }
    }
    
//        double[][] mat = {{2, -1, 3}, {-3, 2, 1}, {-1, 0, 2}};
//        BestMove[][] scores = new BestMove[3][3];
//        for (int i = 0; i < 3; i++)
//            for (int j = 0; j < 3; j++)
//                scores[i][j] = new BestMove(mat[i][j], new GeneralMove(null, 3 * i + j), null);
//        GeneralMove[] moves = {
//            new GeneralMove(null, 1),
//            new GeneralMove(null, 2),
//            new GeneralMove(null, 3)
//        };
//        GeneralMinimax minimax = new GeneralMinimax(new BotState());
//        BestMove best = minimax.computeScore(scores, moves, moves);
//        System.out.println(best.score);
//    }

}
