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
import java.util.stream.Collector;
import java.util.stream.Collectors;

import map.Border;
import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class BotStarter implements Bot 
{
	ArrayList<Border> borders = new ArrayList<>();
	ArrayList<Region> myRegions = new ArrayList<>();

	BorderMinimax minimax;

	ArrayList<AttackTransferMove> attackList = new ArrayList<>();

	@Override
	/**
	 * A method that returns which region the bot would like to start on, the pickable regions are stored in the BotState.
	 * The bots are asked in turn (ABBAABBAAB) where they would like to start and return a single region each time they are asked.
	 * This method returns one random region from the given pickable regions.
	 */
	public Region getStartingRegion(BotState state, Long timeOut)
	{
		minimax = new BorderMinimax(state);

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
	public ArrayList<PlaceArmiesMove> getPlaceArmiesMoves(BotState state, Long timeOut) 
	{
		final LinkedList<Region> visibleRegions = state.getVisibleMap().getRegions();

		System.err.println("Round " + state.getRoundNumber());

		myRegions.clear();
		for (Region region : visibleRegions) {
			region.update();
			if (region.getPlayerName().equals(state.getMyPlayerName())) {
				myRegions.add(region);
			}
		}
		System.err.println("Updated regions");

		myRegions.sort((o1, o2) -> o2.threat - o1.threat);

		System.err.println("Sorted regions");

		ArrayList<PlaceArmiesMove> placeArmiesMoves = new ArrayList<PlaceArmiesMove>();
		String myName = state.getMyPlayerName();
		int armiesLeft = state.getStartingArmies();



		System.err.println("Placed deploy order");
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

		for (Region region: myRegions) {
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

		for (Region region : myRegions) {
			System.err.println(region.getId() + " " + region.distanceToBorder);
		}

		System.err.println("Calculated borders");
		// Sortam dupa dimensiune
		borders.sort((b1, b2) -> b1.getSize() - b2.getSize());
		System.err.println("Finished placing armies.");
		for (PlaceArmiesMove move : placeArmiesMoves) {
			final Region region = move.getRegion();
			region.setArmies(region.getArmies() + move.getArmies());
			region.setMoveableArmies(region.getArmies() - 1);
		}

		attackList.clear();
		// Heuristics for army attack

		List<Region> targets = state.getVisibleMap().getRegions().stream()
				.filter(region1 -> !region1.getPlayerName().equals(myName))
				.map(region2 -> {
					region2.importance = region2.getSuperRegion().getArmiesReward() / region2.getArmies();
					return region2;
				})
				.sorted((Region o1, Region o2) -> o2.importance - o1.importance)
				.collect(Collectors.toList());

		for (Region toRegion : targets) {
			Region fromRegion = toRegion.getNeighbors().stream()
					.filter(region1 -> region1.getPlayerName().equals(myName))
					.sorted((o1, o2) -> o2.getArmies() - o1.getArmies())
					.findFirst().get();

			final int necessaryArmies;

			if (!toRegion.getPlayerName().equals("neutral"))
				necessaryArmies = (int)Math.ceil((5 + toRegion.getArmies()) * 1.8);
			else
				necessaryArmies = (int)Math.ceil(toRegion.getArmies() * 1.8);
			if (fromRegion.getMoveableArmies() < necessaryArmies && armiesLeft > 0) {
				final int count = Math.min(necessaryArmies - fromRegion.getMoveableArmies(), armiesLeft);
				armiesLeft -= count;
				fromRegion.deployArmies(count);
				placeArmiesMoves.add(new PlaceArmiesMove(myName, fromRegion, count));
			}
			if (fromRegion.getMoveableArmies() >= necessaryArmies) {
				attackList.add(new AttackTransferMove(myName, fromRegion, toRegion, necessaryArmies));
				fromRegion.spendArmies(necessaryArmies);
			}
		}


		return placeArmiesMoves;
	}

	@Override
	/**
	 * This method is called for at the second part of each round. This example attacks if a region has
	 * more than 6 armies on it, and transfers if it has less than 6 and a neighboring owned region.
	 * @return The list of PlaceArmiesMoves for one round
	 */
	public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state, Long timeOut) 
	{

		String myName = state.getMyPlayerName();
		double ATTACK_FACTOR = 1.8;
//
//
//		// Minimax
//		//TODO: check that we have 1s in the timebank
//		if (borders.size() > 0 && borders.get(0).getSize() < 7 && state.getRoundNumber() < 10) {
//			System.err.println("Got a border for minmax");
//			System.err.println(borders.get(0).toString());
//			final long startTime = System.nanoTime();
//
//
//
//			final BorderMinimax.Result r = minimax.minimax(borders.get(0), myName, 6);
//			final long endTime = System.nanoTime();
//			System.err.println("Minimax took:" + (double)(endTime - startTime)/1000000 + "ms");
//			System.err.println(Arrays.toString(r.moves.toArray()));
//
//			for (AttackTransferMove m : r.moves) {
//				if (m.getFromRegion().getPlayerName().equals(myName)) {
//					attackTransferMoves.add(m);
//
//					// We don't want the heuristics to touch it
//					state.getVisibleMap().getRegion(m.getFromRegion().getId()).touched = true;
//				}
//			}
//		}






		//Heuristics
		System.err.println("Positioning armies");
		for(final Region fromRegion : state.getVisibleMap().getRegions())
		{
			if(fromRegion.ownedByPlayer(myName) && !fromRegion.touched) //do an attack
			{
				ArrayList<Region> possibleToRegions = new ArrayList<Region>();
				possibleToRegions.addAll(fromRegion.getNeighbors());
				int armiesAvailable = fromRegion.getMoveableArmies();


				// Sorting them based on the less ammount of armies
				possibleToRegions.sort(new Comparator<Region>() {
					@Override
					public int compare(Region o1, Region o2) {
						if (o1.getArmies() == o2.getArmies()) {
							// Preferam regiunile care sunt in aceeasi superregiune
							return ((o1.getSuperRegion().getId() == fromRegion.getId()) ? 0 : 1) -
									((o2.getSuperRegion().getId() == fromRegion.getId()) ? 0 : 1);
						}
						return o1.getArmies() - o2.getArmies();
					}
				});


				// Attacking all the regions that we can
//				for (Region toRegion : possibleToRegions) {
//					final int potentialArmies;
//					if (!toRegion.getPlayerName().equals(myName) && !toRegion.getPlayerName().equals("neutral"))
//						potentialArmies  = toRegion.getArmies() +  5;
//					else
//						potentialArmies = toRegion.getArmies();
//					if (!toRegion.getPlayerName().equals(myName) && armiesAvailable >= potentialArmies * ATTACK_FACTOR) //do an attack
//					{
//						int armiesUsed = (int) Math.ceil(potentialArmies * ATTACK_FACTOR);
//						armiesAvailable -= armiesUsed;
//						attackList.add(new AttackTransferMove(myName, fromRegion, toRegion, armiesUsed));
//					}
//				}

				// Transfering troops to the ones that are close to the border where the real fighting happens
				if (fromRegion.threat  < 5)
					for (Region toRegion : possibleToRegions) {
						if (toRegion.getPlayerName().equals(myName) && armiesAvailable > 0
								&& toRegion.distanceToBorder < fromRegion.distanceToBorder) //do a transfer
						{
							attackList.add(new AttackTransferMove(myName, fromRegion, toRegion, armiesAvailable));
							armiesAvailable = 0;
							break;
						}
					}
			}
		}
		System.err.println("Finished");

		return attackList;
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
