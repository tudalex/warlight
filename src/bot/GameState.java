/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import map.Map;
import move.Move;

import java.util.List;

/**
 *
 * @author Stefan
 */
public class GameState {

    private Map visibleMap;
    private int armiesPerTurn;
    private int opArmiesPerTurn;
    private int round;
    private String myName = "";
    private String opponentName = "";
    public List<Move> opMoves;

    public GameState() {
    }

    public GameState(BotState state) {
        visibleMap = state.getVisibleMap().getMapCopy();
        armiesPerTurn = state.getStartingArmies();
        round = state.getRoundNumber();
        myName = state.getMyPlayerName();
        opponentName = state.getOpponentPlayerName();
        opArmiesPerTurn = state.opTroops;
    }

    public Map getVisibleMap() {
        return visibleMap;
    }

    public void setVisibleMap(Map visibleMap) {
        this.visibleMap = visibleMap;
    }

    public int getArmiesPerTurn() {
        return armiesPerTurn;
    }

    public void setArmiesPerTurn(int armiesPerTurn) {
        this.armiesPerTurn = armiesPerTurn;
    }

    public int getOpArmiesPerTurn() {
        return opArmiesPerTurn;
    }

    public void setOpArmiesPerTurn(int opArmiesPerTurn) {
        this.opArmiesPerTurn = opArmiesPerTurn;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    @Override
    public GameState clone() {
        GameState state = new GameState();
        state.visibleMap = visibleMap.getMapCopy();
        state.armiesPerTurn = armiesPerTurn;
        state.opArmiesPerTurn = opArmiesPerTurn;
        state.round = round;
        state.myName = myName;
        state.opponentName = opponentName;
        return state;
    }

    public String getMyPlayerName() {
        return myName;
    }

    public String getOpponentPlayerName() {
        return opponentName;
    }

}
