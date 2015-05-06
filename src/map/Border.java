/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package map;

import bot.BotState;

import java.util.*;

import move.AttackTransferMove;

/**
 *
 * @author Stefan
 */
public class Border {

    private List<Region> regions = new ArrayList<>();
    private BotState state;
    private Set<AttackTransferMove> prevMoves = new HashSet<>();

    /**
     * Constructs a new border based on the list of original regions. The
     * regions are cloned, and only the neighbours from the border are kept
     *
     * @param regions the original list of regions
     * @param state the bot state
     */
    public Border(List<Region> regions, BotState state) {
        this.state = state;
        java.util.Map<Integer, Region> index = new HashMap<>();
        for (Region r : regions) {
            index.put(r.getId(), new Region(r));
        }
        for (Region r : regions) {
            Region newR = index.get(r.getId());
            for (Region neigh : r.getNeighbors()) {
                if (index.containsKey(neigh.getId())) {
                    newR.addNeighbor(index.get(neigh.getId()));
                }
            }
        }
        this.regions.addAll(index.values());
    }

    /**
     * Constructs a new border based on the starting one and deploys a total of
     * {@code deploy} troops in the regions on the border
     *
     * @param b the old border
     * @param deploy the number of deployed troops
     */
    public Border(Border b, int deploy) {
        this(b.regions, b.state);
        for (Region r : this.regions) {
            if (r.ownedByPlayer(state.getMyPlayerName())) {
                r.setArmies(r.getArmies() + deploy);
            }
        }
    }

    public Border(Border b, AttackTransferMove move) {
        this(b.regions, b.state);
        Region from = null;
        Region to = null;
        for (Region r : regions) {
            if (from == null && r.equals(move.getFromRegion())) {
                from = r;
                continue;

            }
            if (to == null && r.equals(move.getToRegion())) {
                to = r;
                continue;
            }

        }
        if (from.getPlayerName().equals(to.getPlayerName())) {
            from.setArmies(from.getArmies() - move.getArmies());
            from.setMoveableArmies(from.getMoveableArmies() - move.getArmies());
            to.setArmies(to.getArmies() + move.getArmies());
        } else {
            int attacking = Math.max(0, move.getArmies() - move.mostProbableAttackingCasualties());
            int defending = Math.max(0, to.getArmies() - move.mostProbableDefendingCasualties(move.getArmies()));
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
        prevMoves.addAll(b.prevMoves);
        prevMoves.add(move);

    }

    public List<AttackTransferMove> getMoves(String player) {
        String opponent;
        if (player.equals(state.getMyPlayerName())) {
            opponent = state.getOpponentPlayerName();
        } else {
            opponent = state.getMyPlayerName();
        }
        List<AttackTransferMove> moves = new ArrayList<>();
        for (Region start : regions) {
            if (start.ownedByPlayer(player)) {
                for (Region dest : start.getNeighbors()) {
                    if (dest.ownedByPlayer(opponent)
                            && !prevMoves.contains(new AttackTransferMove(player, start, dest, 1)) 
                            && start.getMoveableArmies() > 0) {
                        int d;
                        if (start.getMoveableArmies() < 6) {
                            d = 2;
                        } else {
                            d = 3;
                        }
                        d = start.getMoveableArmies() / d;
                        for (int i = 1; i < start.getMoveableArmies(); i += d) {
                            moves.add(new AttackTransferMove(player, start, dest, i));
                        }
                        moves.add(new AttackTransferMove(player, start, dest, start.getMoveableArmies()));
                    }
                }
            }
        }
        return moves;

    }

    public String getControllingPlayer() {
        String ruler = null;
        for (Region r : regions) {
            if (ruler == null) {
                ruler = r.getPlayerName();
            } else {
                if (!ruler.equals(r.getPlayerName())) {
                    return "";
                }
            }
        }
        return ruler;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public int getSize() {
        return regions.size();
    }

    static public Border getBorder(Region startingRegion, HashSet<Region> visited, BotState state) {
        LinkedList<Region> queue = new LinkedList<>();
        LinkedList<Region> border = new LinkedList<>();
        queue.add(startingRegion);
        while (!queue.isEmpty()) {
            final Region currentRegion = queue.poll();
            for (Region neigh : currentRegion.getNeighbors())
                if (neigh.border && !visited.contains(neigh)) {
                    queue.add(neigh);
                    visited.add(neigh);
                }

            border.add(currentRegion);
        }

        System.err.println("Border " + Arrays.toString(border.toArray()));
        return new Border(border, state);
    }

    @Override
    public String toString() {
        return Arrays.toString(regions.toArray());
    }

}
