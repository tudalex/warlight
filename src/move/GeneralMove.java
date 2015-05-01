/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package move;

import java.util.Objects;
import map.SuperRegion;

/**
 *
 * @author Stefan
 */
public class GeneralMove {
    private SuperRegion superRegion;
    private int number;

    public GeneralMove(SuperRegion superRegion, int number) {
        this.superRegion = superRegion;
        this.number = number;
    }

    public SuperRegion getSuperRegion() {
        return superRegion;
    }

    public void setSuperRegion(SuperRegion superRegion) {
        this.superRegion = superRegion;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return "SuperRegion: " + getSuperRegion().getId() + " numar de trupe: " + getNumber();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.superRegion);
        hash = 67 * hash + this.number;
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
        final GeneralMove other = (GeneralMove) obj;
        if (!Objects.equals(this.superRegion, other.superRegion)) {
            return false;
        }
        if (this.number != other.number) {
            return false;
        }
        return true;
    }
    
    
}
