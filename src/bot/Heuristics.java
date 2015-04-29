package bot;

import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
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

    static Orders greedyHeuristic(BotState state, int armiesLeft, List<Region> regions) {
        final String myName = state.getMyPlayerName();

        final Orders orders = new Orders();
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

			if (!toRegion.getPlayerName().equals("neutral") && state.getRoundNumber() > 80)
				necessaryArmies = (int)Math.ceil((5 + toRegion.getArmies()) * 1.8);
			else
				necessaryArmies = (int)Math.ceil(toRegion.getArmies() * 1.8);
			if (fromRegion.getMoveableArmies() < necessaryArmies && armiesLeft > 0) {
				final int count = Math.min(necessaryArmies - fromRegion.getMoveableArmies(), armiesLeft);
				armiesLeft -= count;
				fromRegion.deployArmies(count);
				orders.placeArmiesMoves.add(new PlaceArmiesMove(myName, fromRegion, count));
			}
			if (fromRegion.getMoveableArmies() >= necessaryArmies) {
				orders.attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, toRegion, necessaryArmies));
				fromRegion.spendArmies(necessaryArmies);
			}
		}

    	return orders;
	}
}
