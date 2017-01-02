import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MyBot {

    private static int myID;
    private static GameMap gameMap;
    private static Random random = new Random();
    private static Direction randomDirection = Direction.STILL;

    public static void main(String[] args) throws java.io.IOException {

        final InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("DinoBot2");

        while(true) {
            List<Move> moves = new ArrayList<Move>();

            Networking.updateFrame(gameMap);
            int width = gameMap.width;
            int height = gameMap.height;
            randomDirection = Direction.CARDINALS[random.nextInt(2)+2];


            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final Location location = gameMap.getLocation(x, y);
                    if(location.getSite().owner == myID) {
                        moves.add(move(location));
                    }
                }
            }
            Networking.sendFrame(moves);
        }
    }

    private static Move move(Location location) {
        final Site site = gameMap.getSite(location);

        boolean[] possible = new boolean[4];
        int numberPossible = 0;
        int numberOwned = 0;

        for (int i=0; i<4; i++) {
            Site toGoSite = gameMap.getSite(location,Direction.CARDINALS[i]);
            if (toGoSite.owner == myID) {
                numberOwned++;
            } else if (toGoSite.strength < site.strength) {
                possible[i] = true;
                numberPossible++;
            }
        }
        if (numberOwned == 4) {
            if (site.strength > 6*site.production)
                return new Move(location, randomDirection);
            return new Move(location, Direction.STILL);
        } else if (numberPossible == 0) {
            return new Move(location, Direction.STILL);
        } else if (numberPossible == 1) {
            for (int i=0; i<4; i++) {
                if (possible[i])
                    return new Move(location, Direction.CARDINALS[i]);
            }
        } else {
            int toGoProduction = 0;
            for (int i=0; i<4; i++) {
                if (possible[i]) {
                    Site toGoSite = gameMap.getSite(location, Direction.CARDINALS[i]);
                    if (toGoSite.production > toGoProduction) {
                        toGoProduction = toGoSite.production;
                    } else {
                        possible[i] = false;
                    }
                }
            }
            for (int i=3; i>=0; i--) {
                if (possible[i]) {
                    return new Move(location, Direction.CARDINALS[i]);
                }
            }
        }
        return new Move(location, Direction.STILL);
    }
}
