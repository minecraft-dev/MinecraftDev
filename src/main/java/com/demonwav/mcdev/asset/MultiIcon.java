package com.demonwav.mcdev.asset;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

public class MultiIcon extends ImageIcon {

    private MultiIcon(final Image image) {
        super(image);
    }

    public static MultiIcon create(final Icon one, final Icon two) {
        Image oneImage = iconToImage(one);
        Image twoImage = iconToImage(two);

        final BufferedImage combinedImage = new BufferedImage(one.getIconWidth(), two.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = combinedImage.createGraphics();
        graphics.drawImage(oneImage, 0, 0, one.getIconWidth() / 2, one.getIconHeight(), 0, 0, one.getIconWidth() / 2, one.getIconHeight(), null);
        graphics.drawImage(twoImage, one.getIconWidth() / 2, 0, two.getIconWidth(), two.getIconHeight(), two.getIconWidth() / 2, 0, one.getIconWidth(), one.getIconHeight(), null);
        graphics.dispose();
        return new MultiIcon(combinedImage);
    }

    // Stolen from http://stackoverflow.com/a/5831357
    private static Image iconToImage(Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon)icon).getImage();
        }
        else {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            BufferedImage image = gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
            Graphics2D g = image.createGraphics();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }
}
