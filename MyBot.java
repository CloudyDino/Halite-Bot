import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MyBot {

    private static int myID;
    private static GameMap gameMap;
    private static Random random = new Random();
    private static boolean randomGoNorthSouth = random.nextBoolean();
    private static Location firstEnemyLocation;

    public static void main(String[] args) throws java.io.IOException {

        final InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        Networking.sendInit("DinoBot4");


        // Until attacked by an enemy and I lose more squares than I gain
        int lastTerritory = 0;
        int currentTerritory = 0;
        while(lastTerritory <= currentTerritory) {
            lastTerritory = currentTerritory;
            currentTerritory = 0;
            firstEnemyLocation = null;
            randomGoNorthSouth = random.nextBoolean();

            List<Move> moves = new ArrayList<Move>();
            Networking.updateFrame(gameMap);

            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    Location location = gameMap.getLocation(x, y);
                    Site site = location.getSite();
                    if(site.owner == myID) {
                        currentTerritory++;
                        if (site.strength > 5*site.production)
                            moves.add(moveToHighProductionStrengthRatio(location));
                    }
                    else if (firstEnemyLocation != null && site.owner != 0) {
                        firstEnemyLocation = location;
                    }
                }
            }
            Networking.sendFrame(moves);
        }

        //End moves to try to kill enemy
        while(true) {
            firstEnemyLocation = null;
            randomGoNorthSouth = random.nextBoolean();

            List<Move> moves = new ArrayList<Move>();
            Networking.updateFrame(gameMap);

            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    final Location location = gameMap.getLocation(x, y);
                    Site site = location.getSite();
                    if(site.owner == myID) {
                        if (site.strength > 5*site.production)
                            moves.add(moveToNearestEnemy(location));
                    }
                    else if (firstEnemyLocation != null && site.owner != 0) {
                        firstEnemyLocation = location;
                    }
                }
            }
            Networking.sendFrame(moves);
        }
    }

    // TYPES OF MOVES

    //Goes to nearest edge if not on the edge
    private static Move moveToHighProductionStrengthRatio(Location loc) {
        Direction toGoDir = Direction.STILL;
        double toGoProduction = 0;
        double toGoStrength = 1;
        for (Direction dir : Direction.CARDINALS) {
            Site possibilitySite = gameMap.getSite(loc, dir);
            if (possibilitySite.owner != myID && ((double)possibilitySite.production)/possibilitySite.strength > toGoProduction/toGoStrength) {
                toGoDir = dir;
                toGoProduction = possibilitySite.production;
                toGoStrength = possibilitySite.strength;
            }
        }
        if (toGoDir == Direction.STILL) {
            return moveToNearestEdge(loc);
        }
        else if (loc.getSite().strength > gameMap.getSite(loc, toGoDir).strength) {
            return new Move(loc, toGoDir);
        }
        else {
            return new Move(loc, Direction.STILL);
        }
    }
    private static Move moveToNearestEdge(Location loc) {
        return new Move(loc, findNearestForeignDirection(loc));
    }
    private static Move moveToNearestEnemy(Location loc) {
        return new Move(loc, findNearestEnemyDirection(loc));
    }

    // TYPES OF LOCATION FINDERS
    private static Location findNearestEnemy(Location loc) {
        return findNearestButAvoid(loc, 0);
    }
    private static Location findNearestForeign(Location loc) {
        return findNearestButAvoid(loc, myID);
    }
    private static Location findNearestButAvoid(Location loc, int avoidID) {
        int maxRadius = Math.min(gameMap.width, gameMap.height)/2;
        for (int radius = 1; radius < maxRadius; radius++) {
            Location possibility;
            if (loc.y-radius < 0)
                possibility = gameMap.getLocation(loc.x, loc.y-radius+gameMap.height);
            else
                possibility = gameMap.getLocation(loc.x, loc.y-radius);
            // Down and to the right
            for (int i = 0; i < radius; i++) {
                possibility = gameMap.getLocation(possibility, Direction.EAST);
                possibility = gameMap.getLocation(possibility, Direction.SOUTH);
                Site possibilitySite = gameMap.getSite(possibility);
                if (possibilitySite.owner != myID && possibilitySite.owner != avoidID) {
                    return possibility;
                }
            }
            // Down and to the left
            for (int i = 0; i < radius; i++) {
                possibility = gameMap.getLocation(possibility, Direction.WEST);
                possibility = gameMap.getLocation(possibility, Direction.SOUTH);
                Site possibilitySite = gameMap.getSite(possibility);
                if (possibilitySite.owner != myID && possibilitySite.owner != avoidID) {
                    return possibility;
                }
            }
            // Up and to the left
            for (int i = 0; i < radius; i++) {
                possibility = gameMap.getLocation(possibility, Direction.WEST);
                possibility = gameMap.getLocation(possibility, Direction.NORTH);
                Site possibilitySite = gameMap.getSite(possibility);
                if (possibilitySite.owner != myID && possibilitySite.owner != avoidID) {
                    return possibility;
                }
            }
            // Up and to the right
            for (int i = 0; i < radius; i++) {
                possibility = gameMap.getLocation(possibility, Direction.EAST);
                possibility = gameMap.getLocation(possibility, Direction.SOUTH);
                Site possibilitySite = gameMap.getSite(possibility);
                if (possibilitySite.owner != myID && possibilitySite.owner != avoidID) {
                    return possibility;
                }
            }
        }
        // If I can't find a place with that, I'll just return the first one found
        if (firstEnemyLocation == null) {
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    final Location location = gameMap.getLocation(x, y);
                    Site site = gameMap.getLocation(x, y).getSite();
                    if(site.owner != myID && site.owner != 0) {
                        firstEnemyLocation = location;
                        return firstEnemyLocation;
                    }
                }
            }
        }
        return firstEnemyLocation;
    }

    // TYPES OF DIRECTION CHOOSER (prefers north and south over east and west)
    private static Direction findNearestEnemyDirection(Location loc) {
        Location toGo = findNearestEnemy(loc);
        return findDirectionToGo(loc, toGo);
    }
    private static Direction findNearestForeignDirection(Location loc) {
        Location toGo = findNearestForeign(loc);
        return findDirectionToGo(loc, toGo);
    }
    private static Direction findDirectionToGo(Location currentLoc, Location toGoLoc) {
        Direction toGoDir = Direction.STILL;
        if (randomGoNorthSouth) {
            if (toGoLoc.y != currentLoc.y) {
                if (toGoLoc.y < currentLoc.y && (currentLoc.y-toGoLoc.y) < (gameMap.height/2))
                    toGoDir =  Direction.NORTH;
                else
                    toGoDir =  Direction.SOUTH;
            }
            else {
                if (toGoLoc.x < currentLoc.x && (currentLoc.x-toGoLoc.x) < (gameMap.width/2))
                    toGoDir =  Direction.WEST;
                else
                    toGoDir =  Direction.EAST;
            }
        }
        else {
            if (toGoLoc.x != currentLoc.x) {
                if (toGoLoc.x < currentLoc.x && (currentLoc.x-toGoLoc.x) < (gameMap.width/2))
                    toGoDir =  Direction.WEST;
                else
                    toGoDir =  Direction.EAST;
            }
            else {
                if (toGoLoc.y < currentLoc.y && (currentLoc.y-toGoLoc.y) < (gameMap.height/2))
                    toGoDir =  Direction.NORTH;
                else
                    toGoDir =  Direction.SOUTH;
            }
        }
        Site toGoSite = gameMap.getLocation(currentLoc, toGoDir).getSite();
        if (toGoSite.owner != myID && gameMap.getSite(currentLoc).strength < toGoSite.strength) {
            return Direction.STILL;
        }
        return toGoDir;
    }
}
