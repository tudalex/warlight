package bot;

import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.GeneralMove;
import move.Move;
import move.PlaceArmiesMove;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by tudalex on 29/04/15.
 */
public class Heuristics {
    static public int enemyArmiesInSuperRegion(SuperRegion superRegion, String playerName) {
        return superRegion.getSubRegions().stream()
                .filter(region -> !region.getPlayerName().equals(playerName))
                .mapToInt(Region::getArmies)
                .sum();
    }

	static ArrayList<Move> metaHeuristic(String myName, int armiesLeft, List<Region> regions, int round, GeneralMove move) {
		HashSet<Region> importantRegions = new HashSet<>();
		move.getSuperRegion().getSubRegions().forEach(region -> importantRegions.addAll(region.getNeighbors()));

		final ArrayList<Move> moves = greedyHeuristic(
				myName,
				move.getNumber(),
				new ArrayList<>(importantRegions),
				round);
		moves.addAll(greedyHeuristic(
				myName,
				armiesLeft,
				regions.stream()
						.filter(region -> !importantRegions.contains(region))
						.collect(Collectors.toList()),
				round));
		return moves;
	}

    static ArrayList<Move> greedyHeuristic(String myName, int armiesLeft, List<Region> regions, int round) {

        final ArrayList<Move> orders = new ArrayList<>();
        final HashMap<Region, Integer> importance = new HashMap<>();
        final HashMap<SuperRegion, Integer> superRegionEnemies = new HashMap<>();
        final HashSet<SuperRegion> superRegions = new HashSet<>();

        // Precalculating SuperRegion information
        regions.stream().forEach(region -> {
            superRegions.add(region.getSuperRegion());
            region.update();
        });
        superRegions.stream().forEach(superRegion ->
				superRegionEnemies.put(superRegion, enemyArmiesInSuperRegion(superRegion, myName)));

		List<Region> targets = regions.stream()
				.filter(region1 -> !region1.getPlayerName().equals(myName))
				.map(region2 -> {
                    final SuperRegion superRegion = region2.getSuperRegion();
					importance.put(
                            region2,
                            superRegion.getArmiesReward() / superRegionEnemies.get(superRegion));
					return region2;
				})
				.sorted((Region o1, Region o2) -> importance.get(o2) - importance.get(o1))
				.collect(Collectors.toList());


		for (Region toRegion : targets) {
			Region fromRegion = toRegion.getNeighbors().stream()
					.filter(region1 -> region1.getPlayerName().equals(myName))
					.sorted((o1, o2) -> o2.getArmies() - o1.getArmies())
					.findFirst().orElse(null);
			if (fromRegion == null)
				continue;
			final int necessaryArmies;

			if (!toRegion.getPlayerName().equals("neutral") && round > 80)
				necessaryArmies = (int)Math.ceil((5 + toRegion.getArmies()) * 1.8);
			else
				necessaryArmies = (int)Math.ceil(toRegion.getArmies() * 1.8);
			if (fromRegion.getMoveableArmies() < necessaryArmies && armiesLeft > 0) {
				final int count = Math.min(necessaryArmies - fromRegion.getMoveableArmies(), armiesLeft);
				armiesLeft -= count;
				fromRegion.deployArmies(count);
				orders.add(new PlaceArmiesMove(myName, fromRegion, count));
			}
			if (fromRegion.getMoveableArmies() >= necessaryArmies) {
				orders.add(new AttackTransferMove(myName, fromRegion, toRegion, necessaryArmies));
				fromRegion.spendArmies(necessaryArmies);
			}
		}

		for(final Region fromRegion : regions)
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

				// Transfering troops to the ones that are close to the border where the real fighting happens
				if (fromRegion.threat  < 5)
					for (Region toRegion : possibleToRegions) {
						if (toRegion.getPlayerName().equals(myName) && armiesAvailable > 0
								&& toRegion.distanceToBorder < fromRegion.distanceToBorder) //do a transfer
						{
							orders.add(new AttackTransferMove(myName, fromRegion, toRegion, armiesAvailable));
							armiesAvailable = 0;
							break;
						}
					}
			}
		}
		System.err.println("Finished");

    	return orders;
	}
}
