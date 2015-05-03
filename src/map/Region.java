/**
 * Warlight AI Game Bot
 *
 * Last update: January 29, 2015
 *
 * @author Jim van Eeden
 * @version 1.1
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */
package map;

import java.util.LinkedList;

public class Region {

    private int id;
    public LinkedList<Region> neighbors;
    private SuperRegion superRegion;
    private int armies;
    private int moveableArmies;
    private String playerName;
    public int threat;
    public boolean border;
    public int distanceToBorder;
    public boolean touched; // Un mod de a marca nodurile ca sa stim cand nu ar trebui sa operam asupra lor
    public int importance;

    public Region(int id, SuperRegion superRegion) {
        this.id = id;
        this.superRegion = superRegion;
        this.neighbors = new LinkedList<Region>();
        this.playerName = "unknown";
        this.armies = 0;
        this.moveableArmies = 0;
        this.threat = 0;
        this.border = false;

        superRegion.addSubRegion(this);
    }

    public Region(int id, SuperRegion superRegion, String playerName, int armies) {
        this.id = id;
        this.superRegion = superRegion;
        this.neighbors = new LinkedList<Region>();
        this.playerName = playerName;
        this.armies = armies;
        superRegion.addSubRegion(this);
    }

    public Region(Region other) {
        this(other.id, other.superRegion, other.playerName, other.armies);
        this.moveableArmies = other.moveableArmies;
    }

    public void deployArmies(int count) {
        armies += count;
        moveableArmies += count;
    }
    public void spendArmies(int count) {
        moveableArmies -= count;
        if (moveableArmies < 0) {
            System.err.println("Shit happened used more armies than available");
        }
    }

    public void update() {
        final String playerName = this.getPlayerName();
        if (playerName.equals("neutral"))
            return;
        this.threat = -this.getArmies() + 1;
        this.border = false;
        for (Region neigh : this.getNeighbors()) {
            if (!neigh.getPlayerName().equals(playerName)) {
                this.threat += neigh.getArmies();
                if (!neigh.getPlayerName().equals("neutral"))
                    this.border = true;
                else
                    this.threat += neigh.getArmies() * 3;
            }
        }
        this.moveableArmies = this.armies - 1;
        if (this.border)
            this.distanceToBorder = 0;
        touched = false;
    }

    public void addNeighbor(Region neighbor) {
        if (!neighbors.contains(neighbor)) {
            neighbors.add(neighbor);
            neighbor.addNeighbor(this);
        }
    }

    /**
     * @param region a Region object
     * @return True if this Region is a neighbor of given Region, false
     * otherwise
     */
    public boolean isNeighbor(Region region) {
        if (neighbors.contains(region)) {
            return true;
        }
        return false;
    }

    /**
     * @param playerName A string with a player's name
     * @return True if this region is owned by given playerName, false otherwise
     */
    public boolean ownedByPlayer(String playerName) {
        if (playerName.equals(this.playerName)) {
            return true;
        }
        return false;
    }

    /**
     * @param armies Sets the number of armies that are on this Region
     */
    public void setArmies(int armies) {
        this.armies = armies;
    }

    public int getMoveableArmies() {
        return moveableArmies;
    }
    
    public void changeSide(String player, int armies) {
        playerName = player;
        this.armies = armies;
        this.moveableArmies = 0;
    }

    public void setMoveableArmies(int moveableArmies) {
        this.moveableArmies = moveableArmies;
    }

    /**
     * @param playerName Sets the Name of the player that this Region belongs to
     */
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /**
     * @return The id of this Region
     */
    public int getId() {
        return id;
    }

    /**
     * @return A list of this Region's neighboring Regions
     */
    public LinkedList<Region> getNeighbors() {
        return neighbors;
    }

    /**
     * @return The SuperRegion this Region is part of
     */
    public SuperRegion getSuperRegion() {
        return superRegion;
    }

    /**
     * @return The number of armies on this region
     */
    public int getArmies() {
        return armies;
    }

    /**
     * @return A string with the name of the player that owns this region
     */
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 31 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Region other = (Region) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "("+this.id + ", " + this.armies + ")";
    }

    
    
}
