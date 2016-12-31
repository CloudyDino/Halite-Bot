import java.util.ArrayList;
import java.util.Random;

public class MyBot {
    public static void main(String[] args) throws java.io.IOException {
        InitPackage iPackage = Networking.getInit();
        int myID = iPackage.myID;
        GameMap gameMap = iPackage.map;

        Networking.sendInit("DinoBot1");

        while(true) {
            ArrayList<Move> moves = new ArrayList<Move>();

            gameMap = Networking.getFrame();
            int width = gameMap.width;
            int height = gameMap.height;

            for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                    Site site = gameMap.getSite(new Location(x, y));
                    Site[] possible = new Site[4];
                    possible[0] = gameMap.getSite(new Location(x, y-1 + (y==0?height:0)));
                    possible[1] = gameMap.getSite(new Location(x+1 -((x+1)/width), y));
                    possible[2] = gameMap.getSite(new Location(x, y+1 - ((y+1)/height)));
                    possible[3] = gameMap.getSite(new Location(x-1 +(x==0?width:0), y));

                    int toGo = 4;
                    int toGoStrength = 266;
                    for (int i=0; i<4; i++) {
                      if (possible[i].owner != myID && possible[i].strength < toGoStrength)
                        toGo = i;
                        toGoStrength = possible[i].strength;
                    }

                    if (toGo == 4) {
                      if (site.strength >= site.production*5 || site.strength > 127) {
                        Direction dir = Direction.CARDINALS[new Random().nextInt(2)];
                        moves.add(new Move(new Location(x, y), dir));
                      }
                      else {
                        moves.add(new Move(new Location(x, y), Direction.STILL));
                      }
                    }
                    else if (site.strength >= toGoStrength){
                      moves.add(new Move(new Location(x, y), Direction.CARDINALS[toGo]));
                    }
                    else {
                      moves.add(new Move(new Location(x, y), Direction.STILL));
                    }
                }
            }
            Networking.sendFrame(moves);
        }
    }
}
