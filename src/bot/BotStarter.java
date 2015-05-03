/**
 * Warlight AI Game Bot
 *
 * Last update: January 29, 2015
 *
 * @author Jim van Eeden
 * @version 1.1
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */

package bot;

/**
 * This is a simple bot that does random (but correct) moves.
 * This class implements the Bot interface and overrides its Move methods.
 * You can implement these methods yourself very easily now,
 * since you can retrieve all information about the match from variable “state”.
 * When the bot decided on the move to make, it returns an ArrayList of Moves. 
 * The bot is started by creating a Parser to which you add
 * a new instance of your bot, and then the parser is started.
 */

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import map.Border;
import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.GeneralMove;
import move.Move;
import move.PlaceArmiesMove;

public class BotStarter implements Bot 
{
	ArrayList<Border> borders = new ArrayList<>();
	ArrayList<Region> myRegions = new ArrayList<>();

    final static boolean GREEDY = false;
	//BorderMinimax minimax;
    final static boolean DEBUG = false;


	ArrayList<Move> orders;

	@Override
	/**
	 * A method that returns which region the bot would like to start on, the pickable regions are stored in the BotState.
	 * The bots are asked in turn (ABBAABBAAB) where they would like to start and return a single region each time they are asked.
	 * This method returns one random region from the given pickable regions.
	 */
	public Region getStartingRegion(BotState state, Long timeOut)
	{
		//minimax = new BorderMinimax(state);

		Region bestRegion = null;
		double bestRatio = -1;
		for (Region pickableRegion : state.getPickableStartingRegions()) {
			SuperRegion superRegion  = pickableRegion.getSuperRegion();
			int sum = 0;
			for (Region region : superRegion.getSubRegions())
				if (region.getId() != pickableRegion.getId())
				{
					// Din cate am inteles ce teritorii nu au 6 armies la start vor deveni wastelands cu 2
					// asa am dedus din gamelog
					sum += (region.getArmies() > 0) ? region.getArmies() : 2;
				}
			final double ratio = (double) sum / superRegion.getArmiesReward();
			if (ratio > bestRatio) {
				bestRatio = ratio;
				bestRegion = pickableRegion;
			}
		}

		return bestRegion;
	}

    @Override
	/**
	 * This method is called for at first part of each round. This example puts two armies on random regions
	 * until he has no more armies left to place.
	 * @return The list of PlaceArmiesMoves for one round
	 */
	public Stream<Move> getPlaceArmiesMoves(BotState state, Long timeOut) {
        final LinkedList<Region> visibleRegions = state.getVisibleMap().getRegions();

        System.err.println("Round " + state.getRoundNumber() + " got " + state.getStartingArmies() + " armies ");

        myRegions.clear();
        for (Region region : visibleRegions) {
            region.update();
            if (region.getPlayerName().equals(state.getMyPlayerName())) {
                myRegions.add(region);
            }
        }


        myRegions.sort((o1, o2) -> o2.threat - o1.threat);


        String myName = state.getMyPlayerName();
        int armiesLeft = state.getStartingArmies();


        HashSet<Region> visited = new HashSet<>();
        borders.clear();

        for (Region region : myRegions) {
            if (region.border && !visited.contains(region)) {
                final Border t = Border.getBorder(region, visited, state);
                borders.add(t);
            }
        }


        visited.clear();

        LinkedList<Region> bfsQueue = new LinkedList<>();

        for (Region region : myRegions) {
            if (region.border) {
                bfsQueue.add(region);
                visited.add(region);
            }
        }

        while (!bfsQueue.isEmpty()) {
            final Region currentRegion = bfsQueue.poll();
            for (Region neigh : currentRegion.getNeighbors())
                if (!visited.contains(neigh) && neigh.getPlayerName().equals(myName)) {
                    visited.add(neigh);
                    neigh.distanceToBorder = currentRegion.distanceToBorder + 1;
                    bfsQueue.add(neigh);
                }
        }
//
//        for (Region region : myRegions) {
//            System.err.println(region.getId() + " " + region.distanceToBorder);
//        }

        // Sortam dupa dimensiune
        borders.sort((b1, b2) -> b1.getSize() - b2.getSize());

        if (DEBUG)
            System.err.println(visibleRegions);
        GeneralMinimax mm = new GeneralMinimax(state);
        GeneralMinimax.BestMove bestMove = mm.minimax(new GameState(state), 1);
        System.err.println("Minimax iterations: "+ mm.iterations);
        System.err.println("Best move: " + bestMove);
        final ArrayList<Move> greedyOrders;
        if (DEBUG)
            System.err.println(visibleRegions);
        if (bestMove.move == null || GREEDY) {
            greedyOrders = Heuristics.greedyHeuristic(state.getMyPlayerName(), armiesLeft, visibleRegions, state.getRoundNumber());
        } else {
            bestMove.move.setSuperRegion(state.getVisibleMap().getSuperRegion(bestMove.move.getSuperRegion().getId()));
            Heuristics.DEBUG = false;
            greedyOrders = Heuristics.metaHeuristic(
                    state.getMyPlayerName(),
                    armiesLeft, visibleRegions, state.getRoundNumber(),
                    bestMove.move);
            Heuristics.DEBUG = false;
        }
        if (DEBUG)
            System.err.println(visibleRegions);
//        if (armiesLeft > 0 && borders.size() > 0) {
//            greedyOrders.add(
//                    new PlaceArmiesMove(myName, borders.get(0).getRegions().get(0), armiesLeft));
//        }
        orders = greedyOrders;
        if (DEBUG)
            System.err.println("Final orders: " + orders);
        return orders.stream().filter(move -> move instanceof PlaceArmiesMove);
    }

    @Override
	/**
	 * This method is called for at the second part of each round. This example attacks if a region has
	 * more than 6 armies on it, and transfers if it has less than 6 and a neighboring owned region.
	 * @return The list of PlaceArmiesMoves for one round
	 */
	public Stream<Move> getAttackTransferMoves(BotState state, Long timeOut)
	{
		return orders.stream().filter(move -> move instanceof AttackTransferMove);
	}

	public static void main(String[] args)
	{
		if (args.length > 0) {
			BorderMinimax.benchmark();
			return;
		}
		BotParser parser = new BotParser(new BotStarter());
		parser.run();
	}

}
