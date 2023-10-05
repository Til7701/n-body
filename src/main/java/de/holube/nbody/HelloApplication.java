package de.holube.nbody;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class HelloApplication {

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        BodyView bodyView = new BodyView(frame);
        BodyController controller = bodyView.getBodyController();

        Model model = new BodyModel();
        controller.setModel(model);
        bodyView.setPreferredSize(new Dimension(model.getViewportWidth(), model.getViewportHeight()));

        frame.add(bodyView);
        frame.pack();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent _windowEvent) {
                model.dispose();
                System.exit(0);
            }
        });
        frame.setVisible(true);

        while (true) {
            model.update();
            controller.repaint();
        }

    }

}