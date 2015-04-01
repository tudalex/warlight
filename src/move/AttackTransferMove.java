/**
 * Warlight AI Game Bot
 *
 * Last update: January 29, 2015
 *
 * @author Jim van Eeden
 * @version 1.1
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */
package move;

import java.util.Objects;
import map.Region;

/**
 * This Move is used in the second part of each round. It represents the attack
 * or transfer of armies from fromRegion to toRegion. If toRegion is owned by
 * the player himself, it's a transfer. If toRegion is owned by the opponent,
 * this Move is an attack.
 */
public class AttackTransferMove extends Move {

    private Region fromRegion;
    private Region toRegion;
    private int armies;

    public AttackTransferMove(String playerName, Region fromRegion, Region toRegion, int armies) {
        super.setPlayerName(playerName);
        this.fromRegion = fromRegion;
        this.toRegion = toRegion;
        this.armies = armies;
    }

    /**
     * @param n Sets the number of armies of this Move
     */
    public void setArmies(int n) {
        armies = n;
    }

    /**
     * @return The Region this Move is attacking or transferring from
     */
    public Region getFromRegion() {
        return fromRegion;
    }

    /**
     * @return The Region this Move is attacking or transferring to
     */
    public Region getToRegion() {
        return toRegion;
    }

    /**
     * @return The number of armies this Move is attacking or transferring with
     */
    public int getArmies() {
        return armies;
    }

    /**
     * @return A string representation of this Move
     */
    public String getString() {
        if (getIllegalMove().equals("")) {
            return getPlayerName() + " attack/transfer " + fromRegion.getId() + " " + toRegion.getId() + " " + armies;
        } else {
            return getPlayerName() + " illegal_move " + getIllegalMove();
        }
    }
    
    @Override
    public String toString() {
        return getString();
    }
    
    public int mostProbableAttackingCasualties() {
        return (int)(toRegion.getArmies() * 0.7); 
    }
    
    public int mostProbableDefendingCasualties() {
        return (int)(armies * 0.6); 
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.fromRegion);
        hash = 37 * hash + Objects.hashCode(this.toRegion);
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
        final AttackTransferMove other = (AttackTransferMove) obj;
        if (!Objects.equals(this.fromRegion, other.fromRegion)) {
            return false;
        }
        if (!Objects.equals(this.toRegion, other.toRegion)) {
            return false;
        }
        return true;
    }
    
    

}
