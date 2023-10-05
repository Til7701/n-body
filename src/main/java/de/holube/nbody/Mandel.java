package de.holube.nbody;

import com.aparapi.Kernel;
import com.aparapi.ProfileInfo;
import com.aparapi.Range;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;


public class Mandel {

    /**
     * User selected zoom-in point on the Mandelbrot view.
     */
    public static volatile Point to = null;

    /**
     * <p>main.</p>
     *
     * @param _args an array of {@link java.lang.String} objects.
     */
    @SuppressWarnings("serial")
    public static void main(String[] _args) {


        final JFrame frame = new JFrame("MandelBrot");

        /** Mandelbrot image height. */
        final Range range = Range.create2D(768, 768);
        System.out.println("range= " + range);

        /** Image for Mandelbrot view. */
        final BufferedImage image = new BufferedImage(range.getGlobalSize(0), range.getGlobalSize(1), BufferedImage.TYPE_INT_RGB);
        final BufferedImage offscreen = new BufferedImage(range.getGlobalSize(0), range.getGlobalSize(1), BufferedImage.TYPE_INT_RGB);
        // Draw Mandelbrot image
        final JComponent viewer = new JComponent() {
            @Override
            public void paintComponent(Graphics g) {

                g.drawImage(image, 0, 0, range.getGlobalSize(0), range.getGlobalSize(1), this);
            }
        };

        // Set the size of JComponent which displays Mandelbrot image
        viewer.setPreferredSize(new Dimension(range.getGlobalSize(0), range.getGlobalSize(1)));

        final Object doorBell = new Object();

        // Mouse listener which reads the user clicked zoom-in point on the Mandelbrot view
        viewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                to = e.getPoint();
                synchronized (doorBell) {
                    doorBell.notify();
                }
            }
        });

        // Swing housework to create the frame
        frame.getContentPane().add(viewer);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Extract the underlying RGB buffer from the image.
        // Pass this to the kernel so it operates directly on the RGB buffer of the image
        final int[] rgb = ((DataBufferInt) offscreen.getRaster().getDataBuffer()).getData();
        final int[] imageRgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        // Create a Kernel passing the size, RGB buffer and the palette.
        final MandelKernel kernel = new MandelKernel(rgb);

        final float defaultScale = 3f;

        // Set the default scale and offset, execute the kernel and force a repaint of the viewer.
        kernel.setScaleAndOffset(defaultScale, -1f, 0f);
        kernel.execute(range);
        System.arraycopy(rgb, 0, imageRgb, 0, rgb.length);
        viewer.repaint();

        System.out.println("device=" + kernel.getTargetDevice());

        // Window listener to dispose Kernel resources on user exit.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent _windowEvent) {
                kernel.dispose();
                System.exit(0);
            }
        });

        // Wait until the user selects a zoom-in point on the Mandelbrot view.
        while (true) {

            // Wait for the user to click somewhere
            while (to == null) {
                synchronized (doorBell) {
                    try {
                        doorBell.wait();
                    } catch (final InterruptedException ie) {
                        ie.getStackTrace();
                    }
                }
            }

            float x = -1f;
            float y = 0f;
            float scale = defaultScale;
            final float tox = ((float) (to.x - (range.getGlobalSize(0) / 2)) / range.getGlobalSize(0)) * scale;
            final float toy = ((float) (to.y - (range.getGlobalSize(1) / 2)) / range.getGlobalSize(1)) * scale;

            // This is how many frames we will display as we zoom in and out.
            final int frames = 128;
            final long startMillis = System.currentTimeMillis();
            for (int sign = -1; sign < 2; sign += 2) {
                for (int i = 0; i < (frames - 4); i++) {
                    scale = scale + ((sign * defaultScale) / frames);
                    x = x - (sign * (tox / frames));
                    y = y - (sign * (toy / frames));

                    // Set the scale and offset, execute the kernel and force a repaint of the viewer.
                    kernel.setScaleAndOffset(scale, x, y);
                    kernel.execute(range);
                    final List<ProfileInfo> profileInfo = kernel.getProfileInfo();
                    if ((profileInfo != null) && (profileInfo.size() > 0)) {
                        for (final ProfileInfo p : profileInfo) {
                            System.out.print(" " + p.getType() + " " + p.getLabel() + " " + (p.getStart() / 1000) + " .. "
                                    + (p.getEnd() / 1000) + " " + ((p.getEnd() - p.getStart()) / 1000) + "us");
                        }
                        System.out.println();
                    }

                    System.arraycopy(rgb, 0, imageRgb, 0, rgb.length);
                    viewer.repaint();
                }
            }

            final long elapsedMillis = System.currentTimeMillis() - startMillis;
            System.out.println("FPS = " + ((frames * 1000) / elapsedMillis));

            // Reset zoom-in point.
            to = null;

        }

    }

    /**
     * An Aparapi Kernel implementation for creating a scaled view of the mandelbrot set.
     *
     * @author gfrost
     */

    public static class MandelKernel extends Kernel {

        /**
         * RGB buffer used to store the Mandelbrot image. This buffer holds (width * height) RGB values.
         */
        final private int rgb[];

        /**
         * Maximum iterations we will check for.
         */
        final private int maxIterations = 64;

        /**
         * Palette maps iteration values to RGB values.
         */
        @Constant
        final private int pallette[] = new int[maxIterations + 1];

        /**
         * Mutable values of scale, offsetx and offsety so that we can modify the zoom level and position of a view.
         */
        private float scale = .0f;

        private float offsetx = .0f;

        private float offsety = .0f;

        /**
         * Initialize the Kernel.
         *
         * @param _rgb Mandelbrot image RGB buffer
         */
        public MandelKernel(int[] _rgb) {
            rgb = _rgb;

            //Initialize palette
            for (int i = 0; i < maxIterations; i++) {
                final float h = i / (float) maxIterations;
                final float b = 1.0f - (h * h);
                pallette[i] = Color.HSBtoRGB(h, 1f, b);
            }

        }

        @Override
        public void run() {

            /** Determine which RGB value we are going to process (0..RGB.length). */
            final int gid = (getGlobalId(1) * getGlobalSize(0)) + getGlobalId(0);

            /** Translate the gid into an x an y value. */
            final float x = (((getGlobalId(0) * scale) - ((scale / 2) * getGlobalSize(0))) / getGlobalSize(0)) + offsetx;

            final float y = (((getGlobalId(1) * scale) - ((scale / 2) * getGlobalSize(1))) / getGlobalSize(1)) + offsety;

            int count = 0;

            float zx = x;
            float zy = y;
            float new_zx = 0f;

            // Iterate until the algorithm converges or until maxIterations are reached.
            while ((count < maxIterations) && (((zx * zx) + (zy * zy)) < 8)) {
                new_zx = ((zx * zx) - (zy * zy)) + x;
                zy = (2 * zx * zy) + y;
                zx = new_zx;
                count++;
            }

            // Pull the value out of the palette for this iteration count.
            rgb[gid] = pallette[count];
        }

        public void setScaleAndOffset(float _scale, float _offsetx, float _offsety) {
            offsetx = _offsetx;
            offsety = _offsety;
            scale = _scale;
        }

    }

}

