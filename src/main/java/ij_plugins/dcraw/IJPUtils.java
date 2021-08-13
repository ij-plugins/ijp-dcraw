/*
 * IJ-Plugins
 * Copyright (C) 2002-2021 Jarek Sacha
 * Author's email: jpsacha at gmail dot com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Latest release available at https://github.com/ij-plugins/ijp-toolkit/
 */

package ij_plugins.dcraw;

import ij.IJ;
import ij.ImageJ;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;


/**
 * Utilities for writing ImageJ plugins.
 *
 * @author Jarek Sacha
 */
public final class IJPUtils {

    private IJPUtils() {
    }


    /**
     * Returns icon used by ImageJ main frame. Returns `null` if main frame is not instantiated or has no icon.
     *
     * @return ImageJ icon or `null`.
     */
    public static java.awt.Image imageJIconAsAWTImage() {
        final ImageJ imageJ = IJ.getInstance();
        return (imageJ != null) ? imageJ.getIconImage() : null;
    }

    /**
     * Create pane for displaying a message that may contain HTML formatting, including links.
     *
     * @param message          the message.
     * @param dialogErrorTitle used in error dialogs.
     * @return component containing the message.
     */
    public static JComponent createHTMLMessageComponent(String message, String dialogErrorTitle) {
        final JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setBorder(null);
        final HTMLDocument htmlDocument = (HTMLDocument) pane.getDocument();
        final Font font = UIManager.getFont("Label.font");
        final String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
        htmlDocument.getStyleSheet().addRule(bodyRule);
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException ex) {
                    IJ.error(dialogErrorTitle, "Error following a link.\n" + ex.getMessage());
                }
            }
        });
        pane.setText(message);
        return pane;
    }

    /**
     * Create simple info panel for a plugin dialog. Intended to be displayed at the top. Includes default logo.
     * Title is displayed in large bold letters. Message can be HTML formatted.
     * <p/>
     * Sample use:
     * <pre>
     *    final GenericDialog dialog = new GenericDialog(TITLE);
     *    dialog.addPanel(IJPUtils.createInfoPanel(TITLE, HTML_DESCRIPTION));
     *    ...
     * </pre>
     *
     * @param title   title displayed in bold font larger than default.
     * @param message message that can contain HTML formatting.
     * @return a panel containing the message with a title and a default icon.
     */
    public static Panel createInfoPanel(final String title, final String message) {

        final Panel rootPanel = new Panel(new BorderLayout(7, 7));
        final Panel titlePanel = new Panel(new BorderLayout(7, 7));

        final JLabel logoLabel = createLogoLabel();
        if (logoLabel != null) {
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titlePanel.add(logoLabel, BorderLayout.WEST);
        }

        final JLabel titleLabel = new JLabel(title);
        final Font font = titleLabel.getFont();
        titleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize() * 2));
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        rootPanel.add(titlePanel, BorderLayout.NORTH);

        final JComponent messageComponent = createHTMLMessageComponent(message, title);
        rootPanel.add(messageComponent, BorderLayout.CENTER);

        // Add some spacing at the bottom
        final JPanel separatorPanel = new JPanel(new BorderLayout());
        separatorPanel.setBorder(new EmptyBorder(7, 0, 7, 0));
        separatorPanel.add(new JSeparator(), BorderLayout.SOUTH);
        rootPanel.add(separatorPanel, BorderLayout.SOUTH);

        return rootPanel;
    }

    /**
     * Load icon as a resource for given class without throwing exceptions.
     *
     * @return Icon or null if loading failed.
     */
    static ImageIcon loadIcon() {
        final Class<?> aClass = IJPUtils.class;
        final String iconPath = "/ij_plugins/dcraw/IJP-48.png";

        try {
            final URL url = aClass.getResource(iconPath);
            if (url == null) {
                IJ.log("Unable to find resource '" + iconPath + "' for class '" + aClass.getName() + "'.");
                return null;
            }
            return new ImageIcon(url);
        } catch (Throwable t) {
            IJ.log("Error loading icon from resource '" + iconPath +
                    "' for class '" + aClass.getName() + "'. \n" + t.getMessage());
            return null;
        }
    }

    private static JLabel createLogoLabel() {
        final ImageIcon logo = IJPUtils.loadIcon();
        if (logo == null) {
            return null;
        } else {
            final JLabel logoLabel = new JLabel(logo, SwingConstants.CENTER);
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            logoLabel.addMouseListener(
                    new MouseAdapter() {
                        private Cursor _oldCursor = null;

                        public void mouseClicked(MouseEvent e) {
                            openLinkInBrowser(DCRawPlugin.HELP_URL);
                        }

                        public void mouseEntered(MouseEvent e) {
                            super.mouseEntered(e);
                            _oldCursor = logoLabel.getCursor();
                            logoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        }

                        public void mouseExited(MouseEvent e) {
                            super.mouseExited(e);
                            logoLabel.setCursor(
                                    Objects.requireNonNullElseGet(_oldCursor, () -> new Cursor(Cursor.DEFAULT_CURSOR)));
                        }
                    });
            return logoLabel;
        }
    }

    private static void openLinkInBrowser(String uri) {
        try {
            openLinkInBrowser(new URI(uri));
        } catch (URISyntaxException ex) {
            IJ.error("Open Link in Browser",
                    "Error creating a link.\n" +
                            "  " + uri + "\n" +
                            ex.getMessage());
        }
    }

    private static void openLinkInBrowser(URI uri) {
        try {
            Desktop.getDesktop().browse(uri);
        } catch (IOException ex) {
            IJ.error("Open Link in Browser",
                    "Error following a link.\n" +
                            "  " + uri.toString() + "\n" +
                            ex.getMessage());
        }
    }
}
