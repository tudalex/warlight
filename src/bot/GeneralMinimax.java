/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import map.Region;
import map.SuperRegion;
import move.GeneralMove;
import move.PlaceArmiesMove;

/**
 *
 * @author Stefan
 */
public class GeneralMinimax {
    
    public BestMove minimax(BotState state, int depth) {
        if (depth == 0) {
            return new BestMove(evaluate(state), null);
        }
        BestMove best = null;
        GeneralMove[] myMoves = getMoves(state, state.getMyPlayerName());
        GeneralMove[] opMoves = getMoves(state, state.getOpponentPlayerName());
        BestMove[][] scores = new BestMove[myMoves.length][opMoves.length];
        for (int i = 0; i < myMoves.length; i++) {
            for (int j = 0; j < opMoves.length; j++) {
                BotState next = nextState(state, myMoves[i], opMoves[j]);
                BestMove score = minimax(next, depth - 1);
                scores[i][j] = score;
            }
        }
        return best;
        
    }
    
    public GeneralMove[] getMoves(BotState state, String player) {
        List<GeneralMove> moves = new ArrayList<>();
        Set<SuperRegion> superRegions = new HashSet<>();
        
        state.getVisibleMap().getRegions().stream().filter((r) -> 
                (r.getPlayerName().equals(player))).forEach((r) -> {
            superRegions.add(r.getSuperRegion());
        });
        superRegions.stream().forEach(sr -> {
            for (int i = 1; i <= state.getStartingArmies(); i++) {
                moves.add(new GeneralMove(sr, i));
            }
        });
        return moves.toArray(new GeneralMove[0]);
    } 
    
    public double evaluate(BotState state) {
        return 0;
    }
    
    
    /**
     * Returns the expected outcome of deploying the armies defined in {@code myMove}
     * and {@code opMove}. The attacks performed by each player is determined using
     * a greedy strategy.
     * @param state The current state of the game
     * @param myMove The deployment of the bot's troops
     * @param opMove The deployment if the opponent's troops
     * @return The next state
     */
    public BotState nextState(BotState state, GeneralMove myMove, GeneralMove opMove) {
        return state;
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
