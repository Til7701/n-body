package de.holube.nbody;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;


public class BodyView extends JComponent {

    private final static GraphicsDevice device = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getScreenDevices()[0];

    private final BodyController bodyController = new BodyController(this);

    private BufferedImage image;

    private boolean fullScreen = false;

    public BodyView(JFrame frame) {
        Canvas canvas = new Canvas();
        this.add(canvas);

        InputMap im = getInputMap(WHEN_FOCUSED);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "onFullScreen");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "onClose");


        am.put("onFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!fullScreen) {
                    device.setFullScreenWindow(frame);
                    fullScreen = true;
                } else {
                    device.setFullScreenWindow(null);
                    fullScreen = false;
                }
            }
        });
        am.put("onClose", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bodyController.getModel().dispose();
                System.exit(0);
            }
        });
    }

    public BodyController getBodyController() {
        return bodyController;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), this);
    }

}
