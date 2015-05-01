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
            return new BestMove(evaluate(state), null);
        }
        GeneralMove[] myMoves = getMoves(state, state.getMyPlayerName());
        GeneralMove[] opMoves = getMoves(state, state.getOpponentPlayerName());
        System.err.println("My moves: "+ myMoves.length);
        System.err.println("Op moves: "+ myMoves.length);
        BestMove[][] scores = new BestMove[myMoves.length][opMoves.length];
        for (int i = 0; i < myMoves.length; i++) {
            for (int j = 0; j < opMoves.length; j++) {
                GameState next = nextState(state, myMoves[i], opMoves[j]);
                BestMove score = minimax(next, depth - 1);
                scores[i][j] = score;
            }
        }
        return computeScore(scores, myMoves);
    }
    
    private BestMove computeScore(BestMove[][] scores, GeneralMove[] myMoves) {
        double[] opScores = new double[scores[0].length];
        for (int i = 0; i < opScores.length; i++) {
            double min = Double.POSITIVE_INFINITY;
            for (int j = 0; j < scores.length; j++) {
                if (scores[j][i].score < min) {
                    min = scores[j][i].score;
                }
                opScores[i] = min;
            }
        }
        double[] opProb = computeProbabilities(opScores, 1);
        double[] myScores = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            double sum = 0;
            for (int j = 0; j < scores[i].length; j++)
                sum += scores[i][j].score * opProb[j];
            myScores[i] = sum;
        }
        double[] myProb = computeProbabilities(myScores, -1);
        double sum = 0;
        for (int i = 0; i < myScores.length; i++) {
            sum += myScores[i] * myProb[i];
        }
        int chosen = choseMove(myProb);
        return new BestMove(sum, myMoves[chosen]);
    }
    
    private double[] computeProbabilities(double[] scores, int sign) {
        double max = Double.NEGATIVE_INFINITY;
        for (double s : scores)
            if (s * sign > max) {
                max = s;
            }
        double sum = 0;
        for (int i = 0; i < scores.length; i++) {
            sum += scores[i] * sign - max;
        }
        double[] prob = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            prob[i] = (scores[i] * sign - max) / sum;
        }
        return prob;
    }
    
    private int choseMove(double[] prob) {
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
                .mapToDouble(sr -> {
                    long myRegions = sr.getSubRegions().stream()
                    .filter(r -> r.getPlayerName().equals(state.getMyPlayerName()))
                    .count();
                    long opRegions = sr.getSubRegions().stream()
                    .filter(r -> r.getPlayerName().equals(state.getOpponentPlayerName()))
                    .count();
                    return Math.pow(2, myRegions * 1. / sr.getSubRegions().size()) * sr.getArmiesReward()
                    - Math.pow(2, opRegions * 1. / sr.getSubRegions().size()) * sr.getArmiesReward();
                }).sum();
        score += state.getVisibleMap().regions.stream().mapToInt(r -> {
            if (!opponent.containsKey(r.getPlayerName())) {
                return 0;
            }
            String op = opponent.get(r.getPlayerName());
            return r.getArmies() - r.getNeighbors().stream().filter(n -> r.getPlayerName().equals(opponent.get(op)))
                    .mapToInt(n -> n.getArmies()).sum();
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
        List<Move> myMoves = Heuristics.metaHeuristic(
                state.getMyPlayerName(),
                myMove.getNumber(),
                myMove.getSuperRegion().getSubRegions(),
                state.getRound(),
                myMove);
        List<Move> opMoves = Heuristics.metaHeuristic(
                state.getOpponentPlayerName(),
                opMove.getNumber(),
                opMove.getSuperRegion().getSubRegions(),
                state.getRound(),
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

        return next;
    }

    private void makeDeployMoves(Map map, List<Move> moves) {
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

        public BestMove(double score, GeneralMove move) {
            this.score = score;
            this.move = move;
        }
    }

}
