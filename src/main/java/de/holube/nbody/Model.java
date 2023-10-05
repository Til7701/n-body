package de.holube.nbody;

public interface Model {

    void update();

    void setRGB(int[] rgb);

    int getViewportWidth();

    int getViewportHeight();

    void dispose();

}
