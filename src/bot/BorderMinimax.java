/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import map.Border;
import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;

/**
 *
 * @author Stefan
 */
public class BorderMinimax {
    
    private Map<String, String> opponent = new HashMap<>();
    private int iterations = 0;
    
    public BorderMinimax(BotState state) {
        opponent.put(state.getMyPlayerName(), state.getOpponentPlayerName());
        opponent.put(state.getOpponentPlayerName(), state.getMyPlayerName());
    }
    
    public Result minimax(Border b, String player, int depth) {
        if (depth == 0) {
            return new Result(evaluate(b, player));
        }
        String ruler = b.getControllingPlayer();
        if (ruler.isEmpty()) {
            Result best = new Result(Double.NEGATIVE_INFINITY);
            for (AttackTransferMove move : b.getMoves(player)) {
                iterations ++;
                Result r = minimax(new Border(b, move), opponent.get(player), depth - 1);
                r.score = -r.score;
                r.moves.push(move);
                if (depth % 2 == 0) r = changeOrder(r, b, player, depth);
                if (r.compareTo(best) > 0) { 
                    best = r;
                }
            }
            if (best.moves.isEmpty()) return minimax(b, opponent.get(player), depth - 1);
            return best;
        }
        if (ruler.equals(player)) return new Result(Double.POSITIVE_INFINITY);
        else return new Result(Double.NEGATIVE_INFINITY);
        
        
    }
    
    public Result changeOrder(Result r, Border b, String player, int depth) {
        AttackTransferMove m1 = r.moves.pop();
        if (r.moves.isEmpty()) {
            r.moves.push(m1);
            return r;
        }
        if (r.moves.peek().getPlayerName().equals(player)) {
            r.moves.push(m1);
            return r;
        }
        AttackTransferMove m2 = r.moves.pop();
        Border next = new Border(new Border(b, m2), m1);
        Result r2 = minimax(next, player, depth - 2);
        double score = (r.score + r2.score) / 2;
        if (Double.isNaN(score)) {
            r.score = 0;
        }
        else {
            r.score = score;
        }
        r.moves.push(m2);
        r.moves.push(m1);
        return r;
    }
    
    public double evaluate(Border b, String player) {
        int sum = 0;
        for (Region r : b.getRegions()) {
            if (r.getPlayerName().equals(player)) sum += r.getArmies();
            if (r.getPlayerName().equals(opponent.get(player))) sum -= r.getArmies();
        }
        return sum;
    }
    
    public static class Result implements Comparable<Result>{
        public Stack<AttackTransferMove> moves = new Stack<>();
        public double score;

        public Result(double score) {
            this.score = score;
        }

        
        
        public Result(AttackTransferMove move, double score) {
            this.moves.push(move);
            this.score = score;
        }

        @Override
        public int compareTo(Result o) {
            return Double.compare(score, o.score);
        }
        
        @Override
        public String toString() {
            return moves + " (" + score + ")";
        }
        
    }
    
    public static void main(String[] args) {
        BotState state = new BotState();
        String[] params = {"settings", "your_bot", "A"};
        state.updateSettings("your_bot", params);
        params[2] = "B";
        state.updateSettings("opponent_bot", params);
        SuperRegion sr = new SuperRegion(1, 1);
        Region r1 = new Region(1, sr);
        Region r2 = new Region(2, sr);
        Region r3 = new Region(3, sr);
        Region r4 = new Region(4, sr);
        r1.addNeighbor(r2);
        r1.addNeighbor(r3);
        r1.addNeighbor(r4);
        r2.addNeighbor(r1);
        r2.addNeighbor(r3);
        r2.addNeighbor(r4);
        r3.addNeighbor(r2);
        r3.addNeighbor(r1);
        r3.addNeighbor(r4);
        r4.addNeighbor(r1);
        r4.addNeighbor(r3);
        r4.addNeighbor(r2);
        
        r1.setArmies(20);
        r2.setArmies(2);
        r3.setArmies(5);
        r4.setArmies(5);
        r1.setPlayerName("A");
        r2.setPlayerName("A");
        r3.setPlayerName("B");
        r4.setPlayerName("B");
        
        List<Region> regions = new ArrayList<>(4);
        regions.add(r1);
        regions.add(r2);
        regions.add(r3);
        regions.add(r4);
        regions.stream().forEach((r) -> {
            r.setMoveableArmies(r.getArmies() - 1);
        });
        Border border = new Border(regions, state);
        
        BorderMinimax minimax = new BorderMinimax(state);
        Result result = minimax.minimax(border, "A", 10);
        System.out.println(result);
        System.out.println(minimax.iterations);
    }
}
