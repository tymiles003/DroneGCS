// License: GPL. For details, see Readme.txt file.
package gui.is.interfaces;

import gui.core.mapObjects.Layer;
import gui.is.classes.Style;

import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

public interface MapObject {

    Layer getLayer();

    void setLayer(Layer layer);

    Style getStyle();

    Style getStyleAssigned();

    Color getColor();

    Color getBackColor();

    Stroke getStroke();

    Font getFont();

    String getName();

    boolean isVisible();
}
