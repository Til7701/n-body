package de.holube.nbody;

import com.aparapi.Kernel;

import java.awt.*;
import java.util.Random;

public class BodyModel extends Kernel implements Model {

    public static final int N = 1000;
    private static final Random RANDOM = new Random();
    private static final float G = 0.01f; // TODO
    private static final float TIME_STEP = 0.001f;

    // Simulation
    private final float[] xPosition = new float[N];
    private final float[] yPosition = new float[N];
    private final float[] zPosition = new float[N];

    private final float[] xVelocity = new float[N];
    private final float[] yVelocity = new float[N];
    private final float[] zVelocity = new float[N];

    private final float[] mass = new float[N];

    private final int viewportWidth = 2736 / 3;
    private final int viewportHeight = 1824 / 3;

    private final int objectColor = Color.HSBtoRGB(0.5f, 1, 1);

    private int[] rgb;


    public BodyModel() {
        setup();
        System.out.println("device=" + this.getTargetDevice());
    }

    public void setup() {
        final int size = viewportHeight / 2;

        for (int i = 0; i < N; i++) {
            xPosition[i] = RANDOM.nextFloat(size) + (size / 2f);
            yPosition[i] = RANDOM.nextFloat(size) + (size / 2f);
            zPosition[i] = 0;

            float speed = 1f;
            xVelocity[i] = RANDOM.nextFloat(speed * 2) - speed;
            yVelocity[i] = RANDOM.nextFloat(speed * 2) - speed;
            zVelocity[i] = RANDOM.nextFloat(speed * 2) - speed;

            mass[i] = RANDOM.nextFloat(1000);
        }

    }

    @Override
    public void update() {
        this.execute(N, 10);
    }

    @Override
    public void setRGB(int[] rgb) {
        this.rgb = rgb;
    }

    @Override
    public int getViewportWidth() {
        return viewportWidth;
    }

    @Override
    public int getViewportHeight() {
        return viewportHeight;
    }

    @Override
    public void run() {
        final int gid = getGlobalId();

        float xAcc = 0f;
        float yAcc = 0f;
        float zAcc = 0f;
        for (int i = 0; i < N; i++) {
            if (i != gid) {
                float xDistance = xPosition[i] - xPosition[gid];
                float xDistanceSquared = xDistance * xDistance;
                float yDistance = yPosition[i] - yPosition[gid];
                float yDistanceSquared = yDistance * yDistance;
                float zDistance = zPosition[i] - zPosition[gid];
                float zDistanceSquared = zDistance * zDistance;

                float distance = sqrt(xDistanceSquared + yDistanceSquared + zDistanceSquared);

                float acceleration = G * (mass[i]) / (distance * distance);

                xAcc += acceleration * (xDistance / distance);
                yAcc += acceleration * (yDistance / distance);
                zAcc += acceleration * (zDistance / distance);
            }
        }

        xVelocity[gid] += xAcc * TIME_STEP;
        yVelocity[gid] += yAcc * TIME_STEP;
        zVelocity[gid] += zAcc * TIME_STEP;

        // update position
        xPosition[gid] += xVelocity[gid] * TIME_STEP;
        yPosition[gid] += yVelocity[gid] * TIME_STEP;
        zPosition[gid] += zVelocity[gid] * TIME_STEP;

        int rgbI = getPositionOnImage(gid);
        if (rgbI >= 0 && rgbI < rgb.length) {
            rgb[rgbI] = objectColor;
        }

    }

    private int getPositionOnImage(int gid) {
        int targetX = (int) xPosition[gid];
        int targetY = (int) yPosition[gid];

        if (targetX < 0 || targetX > viewportWidth || targetY < 0 || targetY > viewportHeight) {
            return -1;
        }

        int i = targetY * viewportWidth;
        return i + targetX;
    }

}
