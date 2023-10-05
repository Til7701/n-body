package de.holube.nbody;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public class BodyController {

    private final BodyView bodyView;

    private Model model;
    private BufferedImage offscreen;
    private BufferedImage image;

    public BodyController(BodyView bodyView) {
        this.bodyView = bodyView;
    }

    public void repaint() {
        final int[] rgb = ((DataBufferInt) offscreen.getRaster().getDataBuffer()).getData();
        final int[] imageRgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        System.arraycopy(rgb, 0, imageRgb, 0, rgb.length);
        Arrays.fill(rgb, Color.HSBtoRGB(0, 1f, 0));
        bodyView.repaint();
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
        offscreen = new BufferedImage(model.getViewportWidth(), model.getViewportHeight(), BufferedImage.TYPE_INT_RGB);
        image = new BufferedImage(model.getViewportWidth(), model.getViewportHeight(), BufferedImage.TYPE_INT_RGB);
        final int[] rgb = ((DataBufferInt) offscreen.getRaster().getDataBuffer()).getData();
        model.setRGB(rgb);
        bodyView.setImage(image);
    }

}