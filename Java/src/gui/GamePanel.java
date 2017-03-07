package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.modeliosoft.modelio.javadesigner.annotations.objid;

@objid ("edc89ef6-b498-483e-875c-befa52d629f4")
public class GamePanel extends JPanel implements ActionListener, PropertyChangeListener {
    @objid ("1630e521-8ea4-48df-9278-b85be1fba591")
    private GameViewer gameViewer;

    @objid ("74e92561-a1a0-467e-a74c-be5e17aa47d1")
    private MainWindow mainWindow;

    @objid ("dbd6f5cb-8848-4a03-9b74-402de0479e51")
    private JLabel timeRemaining;

    @objid ("1c3818b3-5d8a-4afc-a3fe-a21f1c2480f9")
    private JButton btnExit;

    @objid ("c0d6533a-0897-40bb-94e0-4be89488c38b")
    public GamePanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout(0, 0));
        
        JPanel panel = new JPanel();
        add(panel, BorderLayout.NORTH);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        
        JLabel lblNewLabel = new JLabel(" : 2 |  : 3 |  : 4 |   ");
        lblNewLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        panel.add(lblNewLabel);
        
        Component horizontalGlue = Box.createHorizontalGlue();
        panel.add(horizontalGlue);
        
        timeRemaining = new JLabel("0:00 ");
        timeRemaining.setFont(new Font("Dialog", Font.BOLD, 16));
        panel.add(timeRemaining);
        
        btnExit = new JButton("");
        btnExit.addActionListener(this);
        panel.add(btnExit);
        
        gameViewer = new GameViewer();
        add(gameViewer, BorderLayout.CENTER);
    }

    @objid ("20db9f0e-9e0f-43ed-88e1-ac5019fb649c")
    GameViewer getGameViewer() {
        return gameViewer;
    }

    @objid ("3da3d7ef-6623-4412-80f1-1c352bd7f909")
    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == btnExit) {
            mainWindow.showMenu();
        }
    }

    @objid ("28945b2a-0dba-494d-8e43-6992d3e5b089")
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        try {
            GameProperty property = GameProperty.valueOf(evt.getPropertyName());
            
            switch (property) {
                case TimeRemaining:
                    updateTimeRemaining((int) evt.getNewValue());
                    break;
            }
        } catch (IllegalArgumentException e) {}
    }

    @objid ("4eda1677-dd51-4090-9ffa-956332a2fc44")
    private void updateTimeRemaining(int remaining) {
        if (remaining <= 10 && remaining % 2 == 0) //Fait clignoter en rouge
            timeRemaining.setForeground(Color.red);
        else
            timeRemaining.setForeground(Color.black);
        StringBuilder text = new StringBuilder("⌛ ");
        if (remaining < 0)
            text.append("-");
        text.append(Math.abs(remaining/60));
        text.append(':');
        int seconds = Math.abs(remaining%60);
        if (seconds < 10)
            text.append("0");
        text.append(seconds);
        text.append("  ");
        timeRemaining.setText(text.toString());
    }

}
