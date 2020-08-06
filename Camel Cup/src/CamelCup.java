import java.util.Random;
import java.util.Scanner;

public class CamelCup {

    Camel[] camels;

    enum Tile{
        EMPTY,
        OASIS,
        MIRAGE
    }
    //extra space at index 0, for myTile before a tile is placed
    Tile[] tiles;
    int myTile;

    boolean settingUp;
    int numCamelsMoved;

    int numCamels = 5;
    int numTiles = 16; //doesn't include the space behind the starting line

    int[] roundPayoffs = {5, 3, 2};
    int[] gamePayoffs = {8, 5, 3, 2, 1};

    //when the AI simulates every possible round completion, it will simulate a random assortment of endgames
    //this is the total it will simulate per request to calculate game odds
    int endgameSampleSize = 300000;
    //this is the total it will simulate per possible tile to place on the board
    int tileCalculationEndgameSampleSize = 40000;

    public static void main(String[] args) {
        CamelCup camelCup = new CamelCup();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to the Camel Cup AI.");
        System.out.println("First, set up camels' initial positions.");

        camelCup.debugSetup();

        CamelCup backup = new CamelCup(camelCup);

        while (!camelCup.gameFinished()) {
            String line = scanner.next();
            if (line.equals("undo")) {
                camelCup = backup;
                System.out.println("Restored to previous state.");
            }
            else {
                CamelCup tempBackup = new CamelCup(camelCup);
                if (camelCup.handleInput(line))
                    backup = tempBackup;
            }
        }

        System.out.println("The game has finished.");
    }
    public CamelCup() {
        camels = new Camel[numCamels];

        camels[0] = new Camel("blue");
        camels[1] = new Camel("green");
        camels[2] = new Camel("orange");
        camels[3] = new Camel("yellow");
        camels[4] = new Camel("white");

        tiles = new Tile[numTiles + 1];
        clearRound();

        settingUp = true;
    }
    public void debugSetup() {
        handleInput("b1");
        handleInput("g1");
        handleInput("o1");
        handleInput("y1");
        handleInput("w1");
        handleInput("y2");
        handleInput("g3");
        handleInput("b1");
        handleInput("w3");
    }
    private void clearRound() {
        settingUp = false;
        numCamelsMoved = 0;
        for (int i = 0; i < numCamels; i++) {
            camels[i].setHasMoved(false);
            camels[i].resetRoundBettingStage();
        }

        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = Tile.EMPTY;
        }
        myTile = 0;
    }
    public boolean gameFinished() {
        return firstCamel().getPos() > numTiles;
    }
    private Camel firstCamel() {
        Camel posFirst = camels[0];
        for (int i = 1; i < numCamels; i++) {
            if (posFirst.getPos() < camels[i].getPos())
                posFirst = camels[i];
        }
        return posFirst.topCamel();
    }
    private Camel secondCamel(Camel firstCamel) {
        Camel posSecond;
        if (camels[0] != firstCamel)
            posSecond = camels[0];
        else
            posSecond = camels[1];
        for (int i = 1; i < numCamels; i++) {
            if (posSecond.getPos() < camels[i].getPos() && camels[i] != firstCamel)
                posSecond = camels[i];
        }
        Camel output = posSecond.topCamel();
        if (output == firstCamel)
            return output.getBelow();
        return output;
    }
    private Camel lastCamel() {
        Camel posLast = camels[0];
        for (int i = 1; i < numCamels; i++) {
            if (posLast.getPos() > camels[i].getPos())
                posLast = camels[i];
        }
        return posLast.bottomCamel();
    }
    //returns true if command causes an undoable change to the game, and is not just a request for information
    public boolean handleInput(String line) {
        boolean changesTheGame = true;
        if (!settingUp) {
            if (line.equals("disp")) {
                displayTrack();
                changesTheGame = false;
            }
            else if (line.equals("move")) {
                displayMove();
                changesTheGame = false;
            }
            else if (line.equals("rOdds")) {
                displayRoundOdds();
                changesTheGame = false;
            }
            else if (line.equals("gOdds")) {
                displayGameOdds();
                changesTheGame = false;
            }
            else if (line.length() > 3 && line.substring(1).equals("Bet"))
                betCamel(line, false);
            else if (line.length() > 4 && line.substring(1).equals("IBet"))
                betCamel(line, true);
            else if (line.equals("winBet"))
                overallBet(true);
            else if (line.equals("loseBet"))
                overallBet(false);
            else if (line.substring(1).equals("WinBet"))
                overallBet(true, line.charAt(0));
            else if (line.substring(1).equals("LoseBet"))
                overallBet(false, line.charAt(0));
            else if (line.length() > 5 && line.substring(0,5).equals("oasis"))
                placeTile(line.substring(5), Tile.OASIS, false);
            else if (line.length() > 6 && line.substring(0,6).equals("iOasis"))
                placeTile(line.substring(6), Tile.OASIS, true);
            else if (line.length() > 6 && line.substring(0,6).equals("mirage"))
                placeTile(line.substring(6), Tile.MIRAGE, false);
            else if (line.length() > 7 && line.substring(0,7).equals("iMirage"))
                placeTile(line.substring(7), Tile.MIRAGE, true);
            else if (line.length() > 6 && line.substring(0,6).equals("remove"))
                placeTile(line.substring(6), Tile.EMPTY, false);
            else if (line.length() > 7 && line.substring(0,7).equals("iRemove"))
                placeTile(line.substring(7), Tile.EMPTY, true);
            else if (line.length() == 2)
                moveCamel(line);
            else {
                System.out.println("Your input could not be understood. Please try again.");
                changesTheGame = false;
            }
        }
        else {
            if (line.length() == 2)
                moveCamel(line);
            else {
                System.out.println("Your input could not be understood. Please try again.");
                changesTheGame = false;
            }
        }

        if (numCamelsMoved >= numCamels) {
            clearRound();
            System.out.println("A new round begins.");
        }

        return changesTheGame;
    }
    private void betCamel(String line, boolean isMe) {
        Camel betCamel = charToCamel(line.charAt(0));
        if (betCamel == null)
            return;
        if (betCamel.getRoundBettingStage() < roundPayoffs.length) {
            betCamel.incrementRoundBettingStage();
            int newRBS = betCamel.getRoundBettingStage();
            if (isMe) {
                betCamel.placeRoundBet(newRBS - 1);
                if (newRBS < roundPayoffs.length)
                    System.out.println("I took the " + betCamel + " camel's round payoff from " + roundPayoffs[newRBS - 1] + " to " + roundPayoffs[newRBS] + " coins.");
                else
                    System.out.println("I took the " + betCamel + " camel's round payoff from " + roundPayoffs[newRBS - 1] + " to 0 coins.");
            }
            else {
                if (newRBS < roundPayoffs.length)
                    System.out.println("The " + betCamel + " camel's round payoff went from " + roundPayoffs[newRBS - 1] + " to " + roundPayoffs[newRBS] + " coins.");
                else
                    System.out.println("The " + betCamel + " camel's round payoff went from " + roundPayoffs[newRBS - 1] + " to 0 coins.");
            }
        }
        else
            System.out.println("The " + betCamel + " camel can no longer be bet on to win this round.");
    }
    private void moveCamel(String line) {
        Camel moveCamel = charToCamel(line.charAt(0));

        if (moveCamel == null)
            return;

        if (moveCamel.getHasMoved()) {
            System.out.println("This camel has already moved.");
            return;
        }

        int numMoveSpaces;
        switch (line.charAt(1)) {
            case '1':
                numMoveSpaces = 1;
                break;
            case '2':
                numMoveSpaces = 2;
                break;
            case '3':
                numMoveSpaces = 3;
                break;
            default:
                System.out.println("Invalid number of spaces.");
                return;
        }

        moveCamel(moveCamel, numMoveSpaces, false);

        System.out.println("The " + moveCamel + " camel moves forward " + numMoveSpaces + " space" + ((numMoveSpaces > 1) ? "s" : "") + " to space " + moveCamel.getPos() + ".");

        Tile tile = checkForTiles(moveCamel);

        if (tile == Tile.OASIS)
            System.out.println("An oasis advances it to tile " + moveCamel.getPos() + ".");
        else if (tile == Tile.MIRAGE)
            System.out.println("An mirage sets it back to tile " + moveCamel.getPos() + ".");
    }
    private void moveCamel(Camel moveCamel, int numMoveSpaces, boolean fromTile) {
        moveCamel.disengage();

        int newPos = moveCamel.getPos() + numMoveSpaces;

        if (numMoveSpaces > 0) {
            Camel topCamel = topCamel(newPos);
            if (topCamel == null)
                moveCamel.setPos(newPos);
            else
                moveCamel.engage(topCamel);
        }
        else {
            Camel bottomCamel = bottomCamel(newPos);
            if (bottomCamel != null)
                bottomCamel.engage(moveCamel.topCamel());
            moveCamel.setPos(newPos);
        }

        if (!fromTile) {
            moveCamel.setHasMoved(true);
            numCamelsMoved++;
        }
    }
    private Tile checkForTiles(Camel camel) {
        if (camel.getPos() >= tiles.length)
            return Tile.EMPTY;
        Tile tile = tiles[camel.getPos()];
        if (tile == Tile.OASIS)
            moveCamel(camel, 1, true);
        else if (tile == Tile.MIRAGE)
            moveCamel(camel, -1, true);
        return tile;
    }
    private void overallBet(boolean toWin) {
        for (int c = 0; c < numCamels; c++) {
            camels[c].clearProjectedValues();
        }

        int roundSampleSize = roundSampleSize();

        int endgamesPerSimulatedRound = endgameSampleSize / roundSampleSize;
        int actualEndgameSampleSize = (endgamesPerSimulatedRound) * roundSampleSize;

        simulateRestOfRound(endgamesPerSimulatedRound);

        for (int c = 0; c < numCamels; c++) {
            if (toWin)
                camels[c].addToProbableGameWinBettingStage((float) camels[c].getProjectedGameWins() / actualEndgameSampleSize);
            else
                camels[c].addToProbableGameLoseBettingStage((float) camels[c].getProjectedGameLosses() / actualEndgameSampleSize);
        }

        if (toWin)
            System.out.println("A bet has been placed on the overall winner.");
        else
            System.out.println("A bet has been placed on the overall loser.");
    }
    //for when the AI itself bets on the overall winner or loser
    private void overallBet(boolean toWin, char camelChar) {
        Camel camel = charToCamel(camelChar);
        if (camel == null)
            return;
        if (camel.getOverallBet() == Camel.Bet.NONE) {
            camel.overallBet(toWin, probableBetValue(toWin, camel));
            System.out.println("I bet that the " + camel + " camel will be the overall " + (toWin ? "winner." : "loser."));
        }
        else
            System.out.println("I have already placed a bet for this camel.");
    }
    //requires a two-step process for other players to remove and place a tile, but one step for AI
    private void placeTile(String tileNumString, Tile tileType, boolean isMe) {
        try {
            int tileNum = Integer.parseInt(tileNumString);
            placeTile(tileNum, tileType, isMe, true);
        }
        catch (NumberFormatException e) {
            System.out.println("Not a valid number.");
        }
    }
    private void placeTile(int tileNum, Tile tileType, boolean isMe, boolean giveTextFeedback) {
        if (tileType == Tile.EMPTY && tileNum > 1 && tileNum < tiles.length) {
            if (tiles[tileNum] == Tile.EMPTY) {
                if (giveTextFeedback)
                    System.out.println("There is no tile here.");
            }
            else {
                if (isMe) {
                    if (myTile == tileNum) {
                        myTile = 0;
                        tiles[tileNum] = tileType;
                        if (giveTextFeedback)
                            System.out.println("I removed the tile at space " + tileNum + ".");
                    }
                    else if (giveTextFeedback)
                        System.out.println("I can't remove somebody else's tile.");
                }
                else {
                    if (myTile != tileNum) {
                        tiles[tileNum] = tileType;
                        if (giveTextFeedback)
                            System.out.println("A tile was removed from space " + tileNum + ".");
                    }
                    else if (giveTextFeedback)
                        System.out.println("Nobody else can remove my tile.");
                }
            }
        }
        else if (isTilePlaceable(tileNum, isMe)) {
            if (isMe) {
                tiles[myTile] = Tile.EMPTY;
                myTile = tileNum;
                if (giveTextFeedback)
                    System.out.println("I placed " + (tileType == Tile.OASIS ? "an oasis " : "a mirage ") + "tile on space " + tileNum + ".");
            }
            else if (giveTextFeedback)
                System.out.println((tileType == Tile.OASIS ? "An oasis " : "A mirage ") + "tile was placed on space " + tileNum + ".");
            tiles[tileNum] = tileType;
        }
        else if (giveTextFeedback)
            System.out.println("A tile may not be placed here.");
    }
    private boolean isTilePlaceable(int tileNum, boolean isMe) {
        //temporarily removes the AI's tile if the AI is the one making a move
        Tile tempTile = tiles[myTile];
        if (isMe)
            tiles[myTile] = Tile.EMPTY;

        boolean output;

        if (!(tileNum > 1 && tileNum < tiles.length && camelAt(tileNum) == null))
            output = false;
        else if (tileNum != tiles.length - 1 && tiles[tileNum + 1] != Tile.EMPTY)
            output = false;
        else if (tiles[tileNum - 1] == Tile.EMPTY && tiles[tileNum] == Tile.EMPTY)
            output = true;
        else
            output = false;

        if (isMe)
            tiles[myTile] = tempTile;

        return output;
    }
    private Camel charToCamel(char camelChar) {
        switch (camelChar) {
            case 'b':
                return camels[0];
            case 'g':
                return camels[1];
            case 'o':
                return camels[2];
            case 'y':
                return camels[3];
            case 'w':
                return camels[4];
            default:
                System.out.println("No matching camel found.");
                return null;
        }
    }
    private void restoreCamel(Camel moveCamel, int initPos, Camel topCamel, Camel bottomCamel) {
        moveCamel.restore(initPos, topCamel, bottomCamel);

        moveCamel.setHasMoved(false);
        numCamelsMoved--;
    }
    //returns the top camel at pos, or null
    private Camel topCamel(int targetPos) {
        Camel targetPosCamel = camelAt(targetPos);
        if (targetPosCamel == null)
            return null;
        else
            return targetPosCamel.topCamel();
    }
    //returns the bottom camel at pos, or null
    private Camel bottomCamel(int targetPos) {
        Camel targetPosCamel = camelAt(targetPos);
        if (targetPosCamel == null)
            return null;
        else
            return targetPosCamel.bottomCamel();
    }
    //returns the first available camel at pos, or null
    private Camel camelAt(int targetPos) {
        Camel output = null;
        int i = 0;
        while (output == null && i < numCamels) {
            if (camels[i].getPos() == targetPos)
                output = camels[i];
            i++;
        }
        return output;
    }
    private void displayMove() {
        //will output:
        //"Roll"
        //"Bet (camel color) to win round"
        //"Bet (camel color) to win game"
        //"Bet (camel color) to lose game"
        //"Remove tile"
        //"Place an oasis on space (tile number)"
        //"Place a mirage on space (tile number)"

        String output = "Roll";
        float expectedPayoff = 1;
        float newPayoff;

        for (int c = 0; c < numCamels; c++) {
            camels[c].clearProjectedValues();
        }

        int roundSampleSize = roundSampleSize();

        int endgamesPerSimulatedRound = endgameSampleSize / roundSampleSize;
        int actualEndgameSampleSize = (endgamesPerSimulatedRound) * roundSampleSize;

        simulateRestOfRound(endgamesPerSimulatedRound);

        for (int c = 0; c < numCamels; c++) {
            if (camels[c].getRoundBettingStage() < roundPayoffs.length) {
                newPayoff = ((float) roundPayoffs[camels[c].getRoundBettingStage()] * camels[c].getProjectedRoundWins() / roundSampleSize)
                        + ((float) camels[c].getProjectedRoundSeconds() / roundSampleSize) //always a raw payoff of one coin
                        - ((float) (roundSampleSize - camels[c].getProjectedRoundWins() - camels[c].getProjectedRoundSeconds()) / roundSampleSize); //always a raw penalty of one coin
                System.out.println(camels[c] + ": " + newPayoff);
                if (newPayoff > expectedPayoff) {
                    expectedPayoff = newPayoff;
                    output = "Bet " + camels[c] + " to win round";
                }
            }
        }

        for (int c = 0; c < numCamels; c++) {
            if (camels[c].getOverallBet() == Camel.Bet.NONE) {
                newPayoff = (float) probableBetValue(true, camels[c]) * camels[c].getProjectedGameWins() / actualEndgameSampleSize
                        - (float) (actualEndgameSampleSize - camels[c].getProjectedGameWins()) / actualEndgameSampleSize;
                System.out.println(camels[c] + " win: " + newPayoff);
                if (newPayoff > expectedPayoff) {
                    expectedPayoff = newPayoff;
                    output = "Bet " + camels[c] + " to win game";
                }
                newPayoff = (float) probableBetValue(false, camels[c]) * camels[c].getProjectedGameLosses() / actualEndgameSampleSize
                        - (float) (actualEndgameSampleSize - camels[c].getProjectedGameLosses()) / actualEndgameSampleSize;
                System.out.println(camels[c] + " lose: " + newPayoff);
                if (newPayoff > expectedPayoff) {
                    expectedPayoff = newPayoff;
                    output = "Bet " + camels[c] + " to lose game";
                }
            }
        }

        float baseProbableCoins = probableCoins();
        System.out.println("Base: " + baseProbableCoins);

        CamelCup simulatedCup = new CamelCup(this);

        simulatedCup.placeTile(simulatedCup.myTile, Tile.EMPTY, true, false);
        newPayoff = simulatedCup.probableCoins() - baseProbableCoins;
        System.out.println("Remove: " + newPayoff);
        if (newPayoff > expectedPayoff) {
            expectedPayoff = newPayoff;
            output = "Remove tile";
        }

        for (int space = 2; space < tiles.length; space++) {
            if (simulatedCup.isTilePlaceable(space, true)) {
                simulatedCup.placeTile(space, Tile.OASIS, true, false);
                newPayoff = simulatedCup.probableCoins() - baseProbableCoins;
                System.out.println("Oasis " + space + ": " + newPayoff);
                if (newPayoff > expectedPayoff) {
                    expectedPayoff = newPayoff;
                    output = "Place an oasis on space " + space;
                }
                simulatedCup.placeTile(space, Tile.MIRAGE, true, false);
                newPayoff = simulatedCup.probableCoins() - baseProbableCoins;
                System.out.println("Mirage " + space + ": " + newPayoff);
                if (newPayoff > expectedPayoff) {
                    expectedPayoff = newPayoff;
                    output = "Place a mirage on space " + space;
                }
            }
        }

        System.out.println(output);
    }
    private int probableBetValue(boolean toWin, Camel camel) {
        int payoffIndex = Math.round(toWin ? camel.getProbableGameWinBettingStage() : camel.getProbableGameLoseBettingStage());
        if (payoffIndex >= gamePayoffs.length)
            payoffIndex = gamePayoffs.length - 1;
        return gamePayoffs[payoffIndex];
    }
    private void displayTrack() {
        int highestLevel = highestLevel();

        char[][] trackDisplay = new char[highestLevel + 1][numTiles + 1];

        for (int i = 0; i < numCamels; i++) {
            trackDisplay[camels[i].level()][camels[i].getPos()] = camels[i].toString().charAt(0);
        }

        for (int level = highestLevel; level >= 0; level--) {
            for (int space = 1; space <= numTiles; space++) {
                if (trackDisplay[level][space] == (char)0)
                    System.out.print(" ");
                else
                    System.out.print(trackDisplay[level][space]);
                System.out.print("  ");
            }
            System.out.println();
        }

        for (int space = 1; space <= numTiles; space++) {
            System.out.print(space + ((space < 10) ? "  " : " "));
        }

        System.out.println();

        for (int space = 1; space <= numTiles; space++) {
            char tileChar = ' ';
            switch (tiles[space]) {
                case EMPTY:
                    tileChar = ' ';
                    break;
                case OASIS:
                    tileChar = 'O';
                    break;
                case MIRAGE:
                    tileChar = 'M';
                    break;
            }
            System.out.print(tileChar + "  ");

            /*System.out.print(tileChar);
            if (isTilePlaceable(space, false))
                System.out.print("  ");
            else if (isTilePlaceable(space, true))
                System.out.print("? ");
            else
                System.out.print("X ");*/
        }

        System.out.println();

        //System.out.println("My tile: " + myTile);
    }
    private int highestLevel() {
        int output = 0;
        for (int i = 0; i < numCamels; i++) {
            if (output < camels[i].level())
                output = camels[i].level();
        }
        return output;
    }
    private void displayRoundOdds() {
        for (int c = 0; c < numCamels; c++) {
            camels[c].clearProjectedValues();
        }

        int sampleSize = roundSampleSize();

        simulateRestOfRound(0);

        for (int c = 0; c < numCamels; c++) {
            System.out.println(camels[c] + ": "
                    + (float)(10000 * camels[c].getProjectedRoundWins() / sampleSize) / 100 + "% chance of first, "
                    + (float)(10000 * camels[c].getProjectedRoundSeconds() / sampleSize) / 100 + "% chance of second");
        }
    }
    private int roundSampleSize() {
        int output = 1;
        for (int i = 2; i <= numCamels - numCamelsMoved; i++) {
            output *= i;
        }
        return output * (int)(Math.pow(3, numCamels - numCamelsMoved));
    }
    private void displayGameOdds() {
        for (int c = 0; c < numCamels; c++) {
            camels[c].clearProjectedValues();
        }

        int roundSampleSize = roundSampleSize();

        int endgamesPerSimulatedRound = endgameSampleSize / roundSampleSize;
        int actualEndgameSampleSize = endgamesPerSimulatedRound * roundSampleSize;

        simulateRestOfRound(endgamesPerSimulatedRound);

        for (int c = 0; c < numCamels; c++) {
            System.out.println(camels[c] + ": "
                    + (float)(10000 * camels[c].getProjectedGameWins() / actualEndgameSampleSize) / 100 + "% chance of winning, "
                    + (float)(10000 * camels[c].getProjectedGameLosses() / actualEndgameSampleSize) / 100 + "% chance of losing");
        }
    }
    //increments the winning camel's projectedWins after a round finishes
    //numEndgames should be endgamesPerRoundSimulation, or zero for rOdds
    private void simulateRestOfRound(int numEndgames) {
        if (numCamelsMoved >= numCamels) {
            Camel firstCamel = firstCamel();
            firstCamel.incrementProjectedRoundWins();
            secondCamel(firstCamel).incrementProjectedRoundSeconds();
            simulateEndgames(numEndgames);
        }
        else {
            for (int c = 0; c < numCamels; c++) {
                if (!camels[c].getHasMoved()) {
                    Camel topCamel = camels[c].topCamel();
                    Camel bottomCamel = camels[c].bottomCamel();
                    int initPos = camels[c].getPos();

                    for (int s = 1; s <= 3; s++) {
                        moveCamel(camels[c], s, false);
                        checkForTiles(camels[c]);
                        simulateRestOfRound(numEndgames);
                        restoreCamel(camels[c], initPos, topCamel, bottomCamel);
                    }
                }
            }
        }
    }
    private void simulateEndgames(int numEndgames) {
        CamelCup simulatedCup;
        Random rand = new Random();
        for (int endgame = 0; endgame < numEndgames; endgame++) {
            simulatedCup = new CamelCup(this);
            while (!simulatedCup.gameFinished()) {
                if (simulatedCup.numCamelsMoved >= simulatedCup.numCamels)
                    simulatedCup.clearRound();
                int randomCamel = rand.nextInt(simulatedCup.numCamels - simulatedCup.numCamelsMoved);

                int camelI = -1;
                while (randomCamel >= 0) {
                    camelI++;
                    if (!simulatedCup.camels[camelI].getHasMoved())
                        randomCamel--;
                }
                simulatedCup.moveCamel(simulatedCup.camels[camelI], rand.nextInt(3) + 1, false);
                //checking for tiles is unnecessary because there are no tiles in a simulated endgame
            }
            camelWithColor(simulatedCup.firstCamel().toString()).incrementProjectedGameWins();
            camelWithColor(simulatedCup.lastCamel().toString()).incrementProjectedGameLosses();
        }
    }
    private float probableCoins() {
        int roundSampleSize = roundSampleSize();

        int endgamesPerSimulatedRound = tileCalculationEndgameSampleSize / roundSampleSize;
        int actualEndgameSampleSize = endgamesPerSimulatedRound * roundSampleSize;

        long totalCoins = calculateTotalCoins(endgamesPerSimulatedRound);

        return (float)totalCoins / actualEndgameSampleSize;
    }
    private long calculateTotalCoins(int numEndgames) {
        long output = 0;
        if (numCamelsMoved >= numCamels) {
            Camel firstCamel = firstCamel();
            Camel secondCamel = secondCamel(firstCamel);
            for (int c = 0; c < numCamels; c++) {
                if (camels[c] == firstCamel)
                    output += totalRoundPayoff(camels[c], true) * numEndgames;
                else if (camels[c] == secondCamel)
                    output += totalRoundPayoff(camels[c], false) * numEndgames;
                else
                    output -= totalRoundPayoff(camels[c], false) * numEndgames;
            }
            output += calculateEndgameCoins(numEndgames);
        }
        else {
            for (int c = 0; c < numCamels; c++) {
                if (!camels[c].getHasMoved()) {
                    Camel topCamel = camels[c].topCamel();
                    Camel bottomCamel = camels[c].bottomCamel();
                    int initPos = camels[c].getPos();

                    for (int s = 1; s <= 3; s++) {
                        if (camels[c].getPos() + s == myTile) {
                            output += numEndgames;
                        }
                        moveCamel(camels[c], s, false);
                        checkForTiles(camels[c]);
                        output += calculateTotalCoins(numEndgames);
                        restoreCamel(camels[c], initPos, topCamel, bottomCamel);
                    }
                }
            }
        }
        return output;
    }
    private int totalRoundPayoff(Camel camel, boolean isFirst) {
        int output = 0;
        for (int i = 0; i < roundPayoffs.length; i++) {
            if (camel.getHasRoundBet(i)) {
                if (isFirst)
                    output += roundPayoffs[i];
                else
                    output += 1;
            }
        }
        return output;
    }
    private long calculateEndgameCoins(int numEndgames) {
        CamelCup simulatedCup;
        Random rand = new Random(1); //using a common seed ensures an exactly equal chance for all possible tiles
        long output = 0;
        for (int endgame = 0; endgame < numEndgames; endgame++) {
            simulatedCup = new CamelCup(this);
            while (!simulatedCup.gameFinished()) {
                if (simulatedCup.numCamelsMoved >= simulatedCup.numCamels)
                    simulatedCup.clearRound();
                int randomCamel = rand.nextInt(simulatedCup.numCamels - simulatedCup.numCamelsMoved);

                int camelI = -1;
                while (randomCamel >= 0) {
                    camelI++;
                    if (!simulatedCup.camels[camelI].getHasMoved())
                        randomCamel--;
                }
                simulatedCup.moveCamel(simulatedCup.camels[camelI], rand.nextInt(3) + 1, false);
                //checking for tiles is unnecessary because there are no tiles in a simulated endgame
            }
            Camel firstCamel = camelWithColor(simulatedCup.firstCamel().toString());
            Camel lastCamel = camelWithColor(simulatedCup.lastCamel().toString());
            for (int c = 0; c < numCamels; c++) {
                if (camels[c].getOverallBet() == Camel.Bet.WIN) {
                    if (camels[c] == firstCamel)
                        output += camels[c].getProbableBetValue();
                    else
                        output -= 1;
                }
                else if (camels[c].getOverallBet() == Camel.Bet.LOSE) {
                    if (camels[c] == lastCamel)
                        output += camels[c].getProbableBetValue();
                    else
                        output -= 1;
                }
            }
        }
        return output;
    }
    //copy method
    private CamelCup(CamelCup model) {
        settingUp = model.settingUp;
        numCamels = model.numCamels;
        numTiles =  model.numTiles;
        numCamelsMoved = model.numCamelsMoved;

        camels = new Camel[numCamels];

        for (int c = 0; c < numCamels; c++) {
            camels[c] = new Camel(model.camels[c]);
        }

        for (int c = 0; c < numCamels; c++) {
            Camel modelCamel = model.camels[c];
            if (modelCamel.getBelow() != null)
                camels[c].setBelow(camelWithColor(modelCamel.getBelow().toString()));
            if (modelCamel.getAbove() != null)
                camels[c].setAbove(camelWithColor(modelCamel.getAbove().toString()));
        }

        tiles = model.tiles.clone();
        myTile = model.myTile;
    }
    private Camel camelWithColor(String color) {
        for (int c = 0; c < numCamels; c++) {
            if (camels[c].toString().equals(color))
                return camels[c];
        }
        System.err.println("ERROR: no camel with matching color");
        return null;
    }
}