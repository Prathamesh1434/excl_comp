package com.excelutility.gui;

import javax.swing.border.Border;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.awt.Font;

public class UIConstants {

    // Fonts
    public static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD, 28);
    public static final Font FONT_SUBHEADING = new Font("SansSerif", Font.BOLD, 20);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_BUTTON = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_FOOTER = new Font("SansSerif", Font.PLAIN, 10);

    // Colors
    public static final Color COLOR_BACKGROUND = new Color(245, 245, 245);
    public static final Color COLOR_BORDER = new Color(200, 200, 200);
    public static final Color COLOR_PRIMARY_BUTTON = new Color(60, 90, 153);
    public static final Color COLOR_TEXT_HEADER = new Color(50, 50, 50);
    public static final Color COLOR_TEXT_BODY = new Color(80, 80, 80);
    public static final Color COLOR_LINK = new Color(0, 102, 204);

    // Borders
    public static final Border BORDER_PANEL = BorderFactory.createLineBorder(COLOR_BORDER, 1, true);
    public static final Border BORDER_EMPTY_10 = BorderFactory.createEmptyBorder(10, 10, 10, 10);
    public static final Border BORDER_EMPTY_20 = BorderFactory.createEmptyBorder(20, 20, 20, 20);

    // Layout Gaps
    public static final String GAP_10 = "10";
    public static final String GAP_20 = "20";
}
