package gui;

import java.awt.Color;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import com.modeliosoft.modelio.javadesigner.annotations.objid;
import game.AIController;
import game.Client;
import game.Player;
import game.Local;
import game.Server;
import game.World;

@objid ("0352607c-7ee6-4aa1-839f-fc6a174af9fd")
public class GameWorker implements Runnable {
    @objid ("261a2da6-2f7b-4394-83a1-d38b8510f2cb")
    private boolean stop;

    @objid ("e6c9ac41-77db-49aa-b718-1530b3eebbcd")
    private GameSettings settings;

    @objid ("60d0171f-8216-483d-bb98-ccb1fb946a53")
    private MainWindow mainWindow;

    @objid ("f996874c-1764-42a2-867d-851bf61a9f40")
    private World world;

    @objid ("26747bae-5549-4449-8fc3-6aaccd6d8bf3")
    private GamePanel panel;

    @objid ("0c0b3418-de2f-49f0-91d2-008a02cea763")
    private GameViewer viewer;
    
    private Audio audio;

    @objid ("5510e2b1-78a5-4452-a177-88e5ac8f1590")
    public GameWorker(MainWindow mainWindow, GamePanel panel) throws Exception {
        this.settings = GameSettings.getInstance();
        this.mainWindow = mainWindow;
        this.panel = panel;
        this.viewer = panel.getGameViewer();
        createWorld();
    }

    @objid ("57911ebe-31d3-4df2-b871-111cf25912bf")
    public void run() {
        try {
            this.stop = false;
            
            while (!world.isReady()) {
                try {
                    Thread.sleep(1000/settings.fps, 0);
                } catch (InterruptedException e) {  }
            }
            
            final Runnable updateGamePanel = new Runnable() {
                @Override
                public void run() {
                    panel.showGameStatus(world);
                }
            };
            
            viewer.drawMap(world.getMap());
            SwingUtilities.invokeAndWait(updateGamePanel);
            setGameState(GameState.Init);
            
            final long timeStep = 1000000000/settings.fps;
            
            int round = 0;
            while (!stop) {       
                setGameState(GameState.Playing);
                              
                long offset = 0;
                long start = System.nanoTime();
                
                int frame = 0;
                long lastDisplay = System.nanoTime();
                
                while (!stop && !world.isRoundEnded())
                {          
                    if (world.getWarmupTimeRemaining() == 1)
                        mainWindow.clearMessage();
                    
                    world.update();
                    
                    SwingUtilities.invokeLater(updateGamePanel);
                    viewer.drawWorld(world);
                    
                    if (world.getWarmupTimeRemaining() > 0) {
                        final String[] messages = new String[]{"À vos marques.", "Prêts.", "Jouez !"};
                        int i = 3*(world.getWarmupDuration()-world.getWarmupTimeRemaining())/world.getWarmupDuration();
                        SwingUtilities.invokeLater(() -> mainWindow.showMessage(messages[i], Color.black));
                    } else if (world.getTimeRemaining() == 0) {
                        setGameState(GameState.SuddenDeath);
                        SwingUtilities.invokeLater(() -> mainWindow.showMessage("Mort subite !", Color.red));
                    } else if (world.getTimeRemaining() == (int)(-0.75 * world.getFps())) {
                        SwingUtilities.invokeLater(() -> mainWindow.clearMessage());
                    }
                    
                    long duration = System.nanoTime() - start;
                    
                    if (timeStep-duration-offset > 0) {
                        try {
                            Thread.sleep((timeStep-duration-offset)/1000000, (int)(timeStep-duration-offset)%1000000);
                        } catch (InterruptedException e) { }
                    }
                    
                    offset += (System.nanoTime() - start) - timeStep;
                    
                    start = System.nanoTime();
                    
                    frame++;
                    if (System.nanoTime() - lastDisplay >= 1000000000) {
                        System.out.println(frame*(System.nanoTime() - lastDisplay)/1000000000 + " FPS");
                        frame = 0;
                        lastDisplay = System.nanoTime();
                    }
                }
                setGameState(GameState.EndRound);
                
                if (!stop) {
                    if (settings.gameType == GameType.Client && !((Client)world).isConnected())
                    {
                        SwingUtilities.invokeAndWait(() -> mainWindow.showMessage("Déconnecté.", Color.darkGray));
                        try {
                            Thread.sleep(5000, 0);
                        } catch (InterruptedException e) {  }
                    } else if (settings.gameType != GameType.Sandbox) {
                        String message = null;
                        Color color = null;
                        
                        if (world.getPlayerAliveCount() == 1) {
                            PlayerColor[] colors = PlayerColor.values();
                            message = ((Player)world.getPlayers().get(0)).getController().getName() + " gagne !";
                            color = colors[((Player)world.getPlayers().get(0)).getPlayerID() % colors.length].toColor();
                        } else {
                            message = "Égalité !";
                            color = Color.black;
                        }
                        
                        final String x = message; 
                        final Color y = color;
                        SwingUtilities.invokeAndWait(() -> mainWindow.showMessage(x, y)) ;
                    }
                    
                    if (settings.gameType != GameType.Client) {
                        if (round < settings.roundCount-1)
                            world.restart();
                        else
                            stop = true;
                    } else {
                        Client client = (Client)world;
                        while (client.isConnected() && client.isRoundEnded()) {
                            try {
                                Thread.sleep(1000/settings.fps, 0);
                            } catch (InterruptedException e) {  }
                        }
                        
                        stop = !client.isConnected();
                    }
                    
                    mainWindow.clearMessage();
                }
                
                round++;
            }
            setGameState(GameState.End);
        } catch (Exception e) {
            e.printStackTrace(); //Sinon l'erreur n'est jamais propagée
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(panel,
                                                  e.getMessage(),
                                                  "Une erreur est survenue...",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            });
        } finally {
            world.stop();
        }
    }

    @objid ("b5e7e42e-c73b-4130-bed6-7aa32aa55eb3")
    void createWorld() throws Exception {
        if (settings.gameType.equals(GameType.Local) || settings.gameType.equals(GameType.Sandbox) || settings.gameType.equals(GameType.Server)) {
            
            if (settings.gameType.equals(GameType.Local) || settings.gameType.equals(GameType.Sandbox)) {
                world = new Local(settings.mapName+".map",
                                  settings.tileSize,
                                  settings.fps,
                                  settings.duration*settings.fps,
                                  (int)(settings.warmupDuration*settings.fps));
            } else {
                world = new Server(settings.mapName+".map",
                                   settings.tileSize,
                                   settings.fps,
                                   settings.duration*settings.fps,
                                   (int)(settings.warmupDuration*settings.fps));
            }
            
            addKeyboardControllers();
            
            for (int i = 0; i < settings.aiCount; i++) {
                AIController iaController = new AIController();
                world.newController(iaController);
            }
            
            if (settings.gameType.equals(GameType.Local) && world.getPlayerCount() <= 1)
                throw new Exception("Il faut au moins deux joueurs !");
        } else if (settings.gameType.equals(GameType.Client)) {
            world = new Client();
            addKeyboardControllers();
        } else {
            throw new Exception("Non implémenté !");
        }
        
        audio = new Audio();
        world.addGameListener(audio);
    }
    
    private void addKeyboardControllers() {
        for (int i = 0; i < Math.min(settings.playerCount, settings.controls.size()); i++) {
            KeyboardController kbController = new KeyboardController(settings.controls.get(i));
            viewer.addKeyListener(kbController);
            world.newController(kbController);
        }
    }

    @objid ("df3a5c13-59cb-491e-811a-ea1af7e23cda")
    void setGameState(GameState state) {
        System.err.println(state);
    }
    
    @objid ("9f94bd39-ac7e-4e2f-8f19-afc50b48f9ad")
    public void stop() {
        this.stop = true;
    }

}
