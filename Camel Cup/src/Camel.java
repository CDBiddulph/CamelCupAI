public class Camel {

    private String color;
    private int pos;

    private boolean hasMoved;

    private Camel below;
    private Camel above;

    private long projectedRoundWins;
    private long projectedRoundSeconds;
    private long projectedGameWins;
    private long projectedGameLosses;

    //anywhere from 0 to 3, corresponds to indices in roundPayoffs
    private int roundBettingStage;
    boolean[] hasRoundBet;
    //corresponds to indices in gamePayoffs, starts from 0
    private float probableGameWinBettingStage;
    private float probableGameLoseBettingStage;

    enum Bet {
        NONE,
        WIN,
        LOSE
    };
    private Bet overallBet;
    private int probableBetValue;

    public Camel(String aColor) {
        color = aColor;
        pos = 0;
        hasMoved = false;
        below = null;
        above = null;
        projectedRoundWins = 0;
        projectedRoundSeconds = 0;
        projectedGameWins = 0;
        projectedGameLosses = 0;
        roundBettingStage = 0;
        hasRoundBet = new boolean[3];
        probableGameWinBettingStage = 0;
        probableGameLoseBettingStage = 0;
        overallBet = Bet.NONE;
        probableBetValue = 0;
    }
    //clone method
    public Camel(Camel model) {
        color = model.color;
        pos = model.getPos();
        hasMoved = model.getHasMoved();
        below = null;
        above = null;
        projectedRoundWins = model.projectedRoundWins;
        projectedRoundSeconds = model.projectedRoundSeconds;
        projectedGameWins = model.projectedGameWins;
        projectedGameLosses = model.projectedGameLosses;
        roundBettingStage = model.roundBettingStage;
        hasRoundBet = model.hasRoundBet;
        probableGameWinBettingStage = model.probableGameWinBettingStage;
        probableGameLoseBettingStage = model.probableGameLoseBettingStage;
        overallBet = model.overallBet;
        probableBetValue = model.probableBetValue;
    }
    private void updatePos() {
        if (below == null)
            return;
        pos = below.getPos();
    }
    public Camel topCamel() {
        if (above == null)
            return this;
        //System.out.println(above);
        return above.topCamel();
    }
    public Camel bottomCamel() {
        if (below == null)
            return this;
        return below.bottomCamel();
    }
    //disassociates itself with the camel below it
    public void disengage() {
        updatePos();
        if (below != null) {
            below.above = null;
            below = null;
        }
    }
    //associates itself with a new camel below it
    public void engage(Camel other) {
        below = other;
        other.above = this;
    }
    //lowest camel is zero, increments as camels stack up
    public int level() {
        if (below == null)
            return 0;
        else
            return below.level() + 1;
    }
    public void clearProjectedValues() {
        projectedRoundWins = 0;
        projectedRoundSeconds = 0;
        projectedGameWins = 0;
        projectedGameLosses = 0;
    }
    public void incrementProjectedRoundWins() {
        projectedRoundWins++;
    }
    public void incrementProjectedRoundSeconds() {
        projectedRoundSeconds++;
    }
    public void incrementProjectedGameWins() {
        projectedGameWins++;
    }
    public void incrementProjectedGameLosses() {
        projectedGameLosses++;
    }
    public void incrementRoundBettingStage() {
        roundBettingStage++;
    }
    public void resetRoundBettingStage() {
        roundBettingStage = 0;
    }
    public void addToProbableGameWinBettingStage(float amount) {
        probableGameWinBettingStage += amount;
    }
    public void addToProbableGameLoseBettingStage(float amount) {
        probableGameLoseBettingStage += amount;
    }
    public void overallBet(boolean toWin, int aProbableBetValue) {
        if (overallBet == Bet.NONE) {
            overallBet = toWin ? Bet.WIN : Bet.LOSE;
            probableBetValue = aProbableBetValue;
        }
        else
            System.err.println("ERROR: Betting on an already-bet-on camel");
    }
    public void placeRoundBet(int stage) {
        if (stage < 3)
            hasRoundBet[stage] = true;
    }
    public void restore(int aPos, Camel newTopCamel, Camel newBottomCamel) {
        Camel oldBottomCamel = bottomCamel();
        Camel oldTopCamel = topCamel();
        //if this camel's stack is on top of the other one
        if (oldTopCamel == newTopCamel) {
            disengage();
            if (newBottomCamel != this)
                engage(newBottomCamel.topCamel());
        }
        //if this camel's stack is below the other one
        else if (oldBottomCamel == this) {
            newTopCamel.getAbove().disengage();
            if (newBottomCamel != this)
                engage(newBottomCamel.topCamel());
        }
        newBottomCamel.setPos(aPos);
    }
    public String stackString() {
        String output = "";
        Camel current = topCamel();
        while (current.below != null) {
            output += (current + ", ");
        }
        return output + "\b\b";
    }

    //getters/setters

    public String toString() {
        return color;
    }
    public int getPos() {
        updatePos();
        return pos;
    }
    public boolean getHasMoved() {
        return hasMoved;
    }
    public long getProjectedRoundWins() {
        return projectedRoundWins;
    }
    public long getProjectedRoundSeconds() {
        return projectedRoundSeconds;
    }
    public long getProjectedGameWins() {
        return projectedGameWins;
    }
    public long getProjectedGameLosses() {
        return projectedGameLosses;
    }
    public int getRoundBettingStage() {
        return roundBettingStage;
    }
    public boolean getHasRoundBet(int stage) {
        return hasRoundBet[stage];
    }
    public float getProbableGameWinBettingStage() {
        return probableGameWinBettingStage;
    }
    public float getProbableGameLoseBettingStage() {
        return probableGameLoseBettingStage;
    }
    public Bet getOverallBet() {
        return overallBet;
    }
    public int getProbableBetValue() {
        return probableBetValue;
    }
    public Camel getBelow() {
        return below;
    }
    public Camel getAbove() {
        return above;
    }
    public void setPos(int aPos) {
        pos = aPos;
    }
    public void setHasMoved(boolean aHasMoved) {
        hasMoved = aHasMoved;
    }
    public void setBelow(Camel aBelow) {
        below = aBelow;
    }
    public void setAbove(Camel aAbove) {
        above = aAbove;
    }

    //deprecated

    //returns true if this camel is currently beating the other camel
    public boolean isAheadOf(Camel other) {
        if (getPos() > other.getPos())
            return true;
        if (getPos() < other.getPos())
            return false;
        else //if the camels have the same position
            return isAbove(other);
    }
    //returns true if this camel is above the other camel
    public boolean isAbove(Camel other) {
        if (this == other) {
            System.err.println("ERROR: camel compared to itself");
            return false;
        }
        if (getPos() != other.getPos()) {
            System.err.println("ERROR: camels in different positions");
            return false;
        }
        if (below == null)
            return false;
        if (below == other)
            return true;
        return below.isAbove(other);
    }
}
