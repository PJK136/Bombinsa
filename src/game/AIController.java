package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/** Ce contrôleur est l'intelligence artificielle du jeu */
public class AIController extends Controller {
    private WorldView world;
    
    private Direction currentDirection = null;
    
    private int timeElapsedSinceLastShuffle = 0;

    private boolean bombingSimulation = false;
    
    private boolean bombing = false;

    private GridCoordinates aiLocation;
    
    private ArrayList<Direction> directions;

    /**
     * Construit une intelligence artificielle
     */
    public AIController() {
        setName("IA");
        directions = new ArrayList<Direction>(Arrays.asList(Direction.values()));
        Collections.shuffle(directions);
    }
    
    @Override
    public void setCharacter(Character value) {
    	super.setCharacter(value);
		currentDirection = null;
		bombing = false;
		bombingSimulation = false;
    }

    @Override
    public Direction getDirection() {
        if (!bombing) //Avance que si elle ne pose pas de bombe
            return currentDirection;
        else
            return null;
    }

    @Override
    public boolean isPlantingBomb() {
        if (bombing) {
            bombing = false;
            return true;
        } else {
            return false;
        }
    }
    
    private GridCoordinates nextPosition(Direction direction) {
        double x = character.getX();
        double y = character.getY();
        
        switch (direction) {
        case Left:
            x -= character.getMaxSpeed()+(world.getMap().getTileSize()/2.);
            break;
        case Right:
            x += character.getMaxSpeed()+(world.getMap().getTileSize()/2.);
            break;
        case Up:
            y -= character.getMaxSpeed()+(world.getMap().getTileSize()/2.);
            break;
        case Down:
            y += character.getMaxSpeed()+(world.getMap().getTileSize()/2.);
            break;
        default:
            break;
        }
        
        return world.getMap().toGridCoordinates(x, y);
    }

    /**
     * Met à jour l'intelligence artificielle
     */
    @Override
    public void update() {
        world = character.getWorldView();
        aiLocation = world.getMap().toGridCoordinates(character.getX(), character.getY());
              
        if (isReadyToBomb()) {
            bombing = true;
            return;
        }
        
        if (currentDirection == null)
            currentDirection = character.getDirection();
        
        GridCoordinates nextPosition = nextPosition(currentDirection);
        
        timeElapsedSinceLastShuffle++;
        if (timeElapsedSinceLastShuffle >= 0.5*world.getFps()) {
            if (Math.random() < 0.5/world.getFps())
                shuffleDirections();
        }

        if (character.isColliding(currentDirection, character.getMaxSpeed())) {
            shuffleDirections();
            if (!turnSafely(10))
                turnRandomly();
        } else if (!aiLocation.equals(nextPosition)) {
            turnSafely(10);
        }
        
        if (currentDirection != null) {
            Direction opposite = Direction.getOpposite(currentDirection);
            if (directions.get(directions.size()-1) != opposite) {
                directions.remove(opposite);
                directions.add(opposite);
            }
        }
    }
    
    private void shuffleDirections() {
        if (isSafe(aiLocation)) {
            Collections.shuffle(directions);
            timeElapsedSinceLastShuffle = 0;
        }
    }
    
    private boolean turnSafely(int maxStep) {
        return turnSafely(maxStep, -1, null);
    }

    private boolean turnSafely(int maxStep, Direction exclude) {
        return turnSafely(maxStep, -1, exclude);
    }
    
    
    private boolean turnSafely(int maxStep, int minSafe) {
        return turnSafely(maxStep, minSafe, null);
    }
    
    private boolean turnSafely(int maxStep, int minSafe, Direction exclude) {
        DirectionInfo safest = null;
        Direction direction = null;
        
        for (Direction nextDirection : directions) {
            if (character.isColliding(nextDirection, character.getMaxSpeed()))
                continue;
            
            if (nextDirection == exclude)
                continue;
                
            DirectionInfo next = isDirectionSafe(aiLocation, nextDirection, maxStep);
            if (next.compareTo(safest) > 0) {
                safest = next;
                direction = nextDirection;
            }
        }
        
        if ((safest == null || safest.safe <= 0 || safest.step > 1 || safest.badBonus > 0) && isSafe(aiLocation)) {
            currentDirection = null;
            return true;
        } else if (safest != null && safest.safe >= minSafe) {
            currentDirection = direction;
            return true;
        }
        else
            return false;
    }
    
    private void turnRandomly() {
        for (Direction direction : directions) {
            if (currentDirection != direction && !character.isColliding(direction, character.getMaxSpeed())) {
                currentDirection = direction;
                return;
            }
        }
    }
    
    private int getThreateningBombTimeRemaining(GridCoordinates target) {
        int timeRemaining = -1;
        GridCoordinates temp = new GridCoordinates(target);
        
        for(Direction dir : Direction.values()){
            temp = target; // Pas besoin de copier car neighbor donne une autre instance
            while(!world.getMap().isCollidable(temp)){
                Bomb bomb = world.getMap().getFirstBomb(temp);
                
                if(bomb != null && GridCoordinates.distance(target,temp) <= bomb.getRange()) {
                    if (timeRemaining < 0 || bomb.getTimeRemaining() < timeRemaining)
                        timeRemaining = bomb.getTimeRemaining();
                }
                
                if (bombingSimulation && temp.equals(aiLocation)) {
                    if (GridCoordinates.distance(target, aiLocation) <= character.getRange()) {
                        if (timeRemaining == -1)
                            timeRemaining = (int)World.TIME_BEFORE_EXPLOSION*world.getFps();
                        else
                            timeRemaining = Math.min((int)World.TIME_BEFORE_EXPLOSION*world.getFps(), timeRemaining);
                    }
                }
                
                temp = temp.neighbor(dir);
            }
        }
        
        return timeRemaining;
    }

    /**
     * Vérifie la dangerosité d'une tuile
     * @param target La tuile à vérifier
     * @return true Si la tuile est sécurisée, false sinon
     */
    private boolean isSafe(GridCoordinates target) {
        if (world.getMap().isCollidable(target))
            return true;
            
        if(world.getMap().isExploding(target)){
            return false;
        }
        
        Bomb bomb = null;
        GridCoordinates temp = new GridCoordinates(target);
        
        for(Direction dir : Direction.values()){
            temp = target; // Pas besoin de copier car neighbor donne une autre instance
            while(!world.getMap().isCollidable(temp)){
                bomb = world.getMap().getFirstBomb(temp);
                
                if(bomb != null && GridCoordinates.distance(target,temp) <= bomb.getRange())
                    return false;
                
                if (bombingSimulation && temp.equals(aiLocation)) {
                    if (GridCoordinates.distance(target, aiLocation) <= character.getRange())
                        return false;
                }
                
                temp = temp.neighbor(dir);
            }
        }
        
        return true;
    }

    /**
     * Vérifie si une tuile est vide
     * @param gc les coordonnées de grille de la tuile
     * @return true si oui, false sinon
     */
    private boolean isEmpty(GridCoordinates gc) {
        return !world.getMap().isCollidable(gc) && !world.getMap().hasBomb(gc);
    }

    /**
     * Vérifie si une tuile est vide et sécurisée
     * @param gc les coordonnées de grille de la tuile
     * @return true si oui, false sinon
     */
    private boolean isEmptyAndSafe(GridCoordinates gc) {
        return isEmpty(gc) && isSafe(gc);
    }

    private boolean isBadBonus(GridCoordinates gc) {
        if (!world.getMap().isInsideMap(gc) || world.getMap().getTileType(gc) != TileType.Bonus)
            return false;
        
        return !world.getMap().getBonusType(gc).isGood();
    }
    
    private boolean isGoodBonus(GridCoordinates gc) {
        if (!world.getMap().isInsideMap(gc) || world.getMap().getTileType(gc) != TileType.Bonus)
            return false;
        
        return world.getMap().getBonusType(gc).isGood();
    }
    
    private boolean hasTarget(int range) {
        boolean destroyGoodBonus = false;
        boolean destroyBreakable = false;
        boolean hitCharacter = false;
        
        for (Direction direction : Direction.values()) {
        	GridCoordinates position = aiLocation;
            for (int i = 0; i < range; i++) {
            	position = position.neighbor(direction);
                if (!world.getMap().isInsideMap(position))
                	break;
                else if (world.getMap().getTileType(position) == TileType.Breakable) {
                    destroyBreakable = true;
                    break;
                }
                else if (!isEmpty(position))
                    break;
                else if (isGoodBonus(position))
                	destroyGoodBonus = true;
                else {
                	List<Entity> entities = world.getMap().getEntities(position);
                	for (Entity entity : entities) {
                		if (entity instanceof Character && entity != character)
                			return true;
                	}
                }
            }
        }
        
        if (hitCharacter)
            return true;
        else if (!destroyGoodBonus && destroyBreakable)
            return true;
        else
            return false;
    }
    
    /**
     * Détermine si l'IA peut poser une bombe dans la situation actuelle
     * @return true si oui, false sinon
     */
    private boolean isReadyToBomb() {
        if(character.getBombCount() < character.getBombMax() && world.getTimeRemaining()>0 && isSafe(aiLocation)
        		&& Math.random() < 2./world.getFps() && hasTarget(character.getRange())) {
            Direction beforeSimulation = currentDirection;
            bombingSimulation = true;
            if (turnSafely(5, 1)) {
                bombingSimulation = false;
                return true;
            } else {
                bombingSimulation = false;
                currentDirection = beforeSimulation;
            }
        }
        return false;
    }

    private static class DirectionInfo implements Comparable<DirectionInfo> {
        public int safe = -3;
        public int step = 0;
        public int badBonus = 0;
        public int goodBonus = 0;
        
        public DirectionInfo() {
        }
        
        public DirectionInfo(int safe, int step, int badBonus, int goodBonus) {
            this.safe = safe;
            this.step = step;
            this.badBonus = badBonus;
            this.goodBonus = goodBonus;
        }
        
        @Override
        public int compareTo(DirectionInfo o) {
            if (o == null)
                return 1;

            if (safe < o.safe)
                return -1;
            else if (safe > o.safe)
                return 1;
            
            else if (badBonus > o.badBonus)
                return -1;
            else if (badBonus < o.badBonus)
                return 1;
            
            else if (step > o.step)
                return -1;
            else if (step < o.step)
                return 1;
            
            else if (goodBonus < o.goodBonus)
                return -1;
            else if (goodBonus > o.goodBonus)
                return 1;
            
            return 0;
        }
    }
    
    private DirectionInfo isDirectionSafe(GridCoordinates position, Direction direction, int maxStep) {
        return isDirectionSafe(position, direction, maxStep, 0);
    }
    
    private DirectionInfo isDirectionSafe(GridCoordinates position, Direction direction, int maxStep, int step) {      
        GridCoordinates nextPosition = position.neighbor(direction);
        DirectionInfo ret = new DirectionInfo(0, 1, isBadBonus(nextPosition) ? 1 : 0, isGoodBonus(nextPosition) ? 1 : 0);
        
        if (!isEmpty(nextPosition))
            ret.safe = -1;
        else if (world.getMap().isExploding(nextPosition)) {
            if (world.getMap().getExplosionTimeRemaining(nextPosition) <= (step-1)*world.getMap().getTileSize()/character.getMaxSpeed())
                ret.safe = 1;
            else
                ret.safe = -2;
        }
        else {
            int timeRemaining = getThreateningBombTimeRemaining(nextPosition);
            
            if (timeRemaining < 0) { //Safe
                ret.safe = 1;
                
                for (int i = step; i < maxStep; i++) { //Compte les bons bonus si l'on suit cette voie 
                	nextPosition = nextPosition.neighbor(direction);
                	
                	if (!isEmpty(nextPosition) || world.getMap().isExploding(nextPosition) ||
                	        isBadBonus(nextPosition) || !isSafe(nextPosition))
                    	break;
                    else if (isGoodBonus(nextPosition))
                    	ret.goodBonus++;
                }
            } else if (step+1 >= maxStep) { //Inconnu mais nb de pas max atteint
                ret.safe = 0;
            } else { //Vide mais unsafe
                if ((step+1)*world.getMap().getTileSize()/character.getMaxSpeed() > timeRemaining) {
                    ret.safe = -2;
                }
                else {
                    DirectionInfo safest = null;
                    
                    for (Direction nextDirection : directions) {
                        if (Direction.areOpposite(direction, nextDirection))
                            continue;
                        
                        DirectionInfo next = isDirectionSafe(nextPosition, nextDirection, maxStep, step+1);
                        if (next.compareTo(safest) > 0)
                            safest = next;
                    }
                    
                    ret.safe = safest.safe;
                    ret.step += safest.step;
                    ret.badBonus += safest.badBonus;
                    ret.goodBonus += safest.goodBonus;
                }
            }
        }
        
        return ret;
    }
}