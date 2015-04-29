package bot;

import move.AttackTransferMove;
import move.Move;
import move.PlaceArmiesMove;

import java.util.ArrayList;

/**
 * Created by tudalex on 29/04/15.
 */
public class Orders {

    public ArrayList<PlaceArmiesMove> placeArmiesMoves;
    public ArrayList<AttackTransferMove> attackTransferMoves;

    Orders() {
        placeArmiesMoves = new ArrayList<>();
        attackTransferMoves = new ArrayList<>();
    }

    Orders(ArrayList<PlaceArmiesMove> thePlaceArmiesMoves, ArrayList<AttackTransferMove> theAttackTransferMoves) {
        placeArmiesMoves = thePlaceArmiesMoves;
        attackTransferMoves = theAttackTransferMoves;
    }

    public ArrayList<Move> getMoveList() {
        final ArrayList<Move> ret =  new ArrayList<>();
        ret.addAll(placeArmiesMoves);
        ret.addAll(attackTransferMoves);
        return ret;
    }

}
