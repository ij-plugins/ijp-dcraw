/***
 * Image/J Plugins
 * Copyright (C) 2002-2006 Jarek Sacha
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
 * Latest release available at http://sourceforge.net/projects/ij-plugins/
 */
package net.sf.ij_plugins.dcraw;

import ij.*;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PGM_Reader;
import ij.plugin.PlugIn;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Plugin for opening RAW images. It calls DCRAW to convert a RAW image to PPM then loads that PPM image.
 * <p/>
 * The home site for DCRAW is http://www.cybercom.net/~dcoffin/dcraw/.
 *
 * @author Jarek Sacha
 */
public class DCRawPlugin implements PlugIn {

    private static final String PROPERTY_DCRAW_BIN = "dcrawExecutable.path";
    private static final String TITLE = "DCRaw Reader";
    private static final String ABOUT =
            "The Digital Camera Raw Reader opens over 200 raw image formats using DCRAW program created by\n" +
                    "David Coffin. Full list of supported cameras can be found at DCRAW home page:\n" +
                    "   http://www.cybercom.net/~dcoffin/dcraw/\n" +
                    "\n" +
                    "The Reader requires the DCRAW binary. Versions for various operating systems\n" +
                    "can be downloaded through the Reader home page:\n" +
                    "    http://ij-plugins.sourceforge.net/plugins/dcraw\n" +
                    "or through DCRAW home page.\n" +
                    "By default, the Reader looks for the DCRAW binary in subdirectory 'dcraw'\n" +
                    "of ImageJ plugins folder. Alternative location can be specified by adding\n" +
                    "'" + Prefs.KEY_PREFIX + PROPERTY_DCRAW_BIN + "' to ImageJ properties file IJ_Props.txt located in ImageJ\n" +
                    "home directory. Example line that should be added to IJ_Props.txt:\n" +
                    "    " + Prefs.KEY_PREFIX + PROPERTY_DCRAW_BIN + "=/apps/bin/dcraw.exe\n" +
                    "Reading of 48 bit RGB images requires ImageJ v.1.35p or newer.";


    public void run(final String arg) {

        final String title = TITLE + " (v." + DCRawVersion.getInstance() + ")";

        if ("about".equalsIgnoreCase(arg)) {
            IJ.showMessage("About " + title, ABOUT);
            return;
        }

        // Establish location of DCRAW executable
        final File dcrawFile;
        try {
            dcrawFile = locateDCRAW();
        } catch (DCRawWrapperException e) {
            e.printStackTrace();
            IJ.error(title, e.getMessage());
            IJ.showMessage("About " + title, ABOUT);
            return;
        }

        if (!dcrawFile.exists()) {
            IJ.error("Invalid path to DCRAW executable: '" + dcrawFile.getAbsolutePath() + "'.");
            IJ.showMessage("About " + title, ABOUT);
            return;
        }

        log("DCRAW binary: " + dcrawFile.getAbsolutePath());

        // Ask for location of the RAW file to read
        final OpenDialog openDialog = new OpenDialog("Open", null);
        if (openDialog.getFileName() == null) {
            // No selection
            return;
        }

        final File rawFile = new File(openDialog.getDirectory(), openDialog.getFileName());
        IJ.showStatus("Opening RAW file: " + rawFile.getName());

        // Check if PPM file existed before it will be written created by DCRAW
        final File ppmFile = new File(rawFile.getParentFile(), toPPMFileName(rawFile.getName()));
        final boolean removePPM = !ppmFile.exists();

        // Ask for DCRAW options
        final GenericDialog dialog = new GenericDialog(title);

        // Auto whitebalance
        dialog.addCheckbox("Use automatic whitebalance", false);

        // Image format
        final String[][] formatChoice = {
                {"8-bit non-linear", "16-bit linear"},
                {"-2", "-4"}
        };
        dialog.addChoice("Read as", formatChoice[0], formatChoice[0][0]);

        // Interpolation quality
        final String[] interpolationQuality = {"0", "1", "2", "3"};
        dialog.addChoice("Interpolation quality", interpolationQuality, interpolationQuality[3]);

        dialog.showDialog();

        if (dialog.wasCanceled()) {
            // No selection
            return;
        }

        // Command line components
        final List commandList = new ArrayList();

        // First put DCRAW executable
        commandList.add(dcrawFile.getAbsolutePath());
        // Turn on verbose messages
        commandList.add("-v");

        // Add options
        if (dialog.getNextBoolean()) {
            commandList.add("-a");
        }
        commandList.add(formatChoice[1][dialog.getNextChoiceIndex()]);
        commandList.add("-q");
        commandList.add(dialog.getNextChoice());

        // Add input PPM
        commandList.add(rawFile.getAbsolutePath());

        // Run DCRAW
        final String[] command = (String[]) commandList.toArray(new String[commandList.size()]);
        {
            final StringBuffer commandOptions = new StringBuffer();
            for (int i = 1; i < command.length; i++) {
                commandOptions.append(command[i]).append(" ");
            }
            log("DCRAW command line: " + commandOptions);
        }

        try {
            executeCommand(command);
        } catch (DCRawWrapperException e) {
            e.printStackTrace();
            IJ.error(e.getMessage());
            IJ.showMessage("About " + title, ABOUT);
            return;
        }

        // Read PPM file
        if (!ppmFile.exists()) {
            IJ.error("Unable to locate DCRAW output PPM file: '" + ppmFile.getAbsolutePath() + "'.");
            return;
        }
        IJ.showStatus("Opening: " + ppmFile.getAbsolutePath());
        final PGM_Reader reader = new PGM_Reader();
        final ImageStack stack;
        try {
            stack = reader.openFile(ppmFile.getAbsolutePath());
        } catch (IOException e) {
            IJ.error(title, e.getMessage());
            if (removePPM) {
                ppmFile.delete();
            }
            return;
        }

        final ImagePlus imp;
        imp = new ImagePlus(ppmFile.getName(), stack);
        imp.show();

        // Remove PPM if it did not exist
        if (removePPM) {
            ppmFile.delete();
        }
    }

    private static void log(final String message) {
        if (IJ.debugMode) {
            IJ.log(message);
        }
    }

    private static String toPPMFileName(final String rawFileName) {
        final int dotIndex = rawFileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return rawFileName + ".ppm";
        } else {
            return rawFileName.substring(0, dotIndex) + ".ppm";
        }
    }

    private static File locateDCRAW() throws DCRawWrapperException {
        final File dcrawBinFile;
        if (System.getProperty(PROPERTY_DCRAW_BIN, null) != null) {
            // Try to read from a system property
            final String path = System.getProperty(PROPERTY_DCRAW_BIN);
            dcrawBinFile = new File(path);
            if (!dcrawBinFile.exists()) {
                throw new DCRawWrapperException("System property '" + PROPERTY_DCRAW_BIN
                        + "' does not point to an existing DCRAW executable ["
                        + dcrawBinFile.getAbsolutePath() + "]");
            }
        } else if (Prefs.get(PROPERTY_DCRAW_BIN, null) != null) {
            // Try to read from ImageJ properties
            final String path = Prefs.get(PROPERTY_DCRAW_BIN, null);
            dcrawBinFile = new File(path);
            if (!dcrawBinFile.exists()) {
                throw new DCRawWrapperException("ImageJ property '" + PROPERTY_DCRAW_BIN
                        + "' (IJ_Prefs.txt) does not point to an existing DCRAW executable ["
                        + dcrawBinFile.getAbsolutePath() + "]");
            }
        } else if (Menus.getPlugInsPath() != null) {
            // Try to locate in plugins directory
            final String path = Menus.getPlugInsPath() + File.separator
                    + (IJ.isWindows() ? "dcraw/dcraw.exe" : "dcraw/dcraw");
            dcrawBinFile = new File(path);
            if (!dcrawBinFile.exists()) {
                throw new DCRawWrapperException(
                        "Unable to find DCRAW binary in ImageJ plugins folder. File does not exist: '"
                                + dcrawBinFile.getAbsolutePath() + "'.");
            }
        } else {
            throw new DCRawWrapperException("Unable to find DCRAW binary.");
        }

        return dcrawBinFile;
    }

    private static String executeCommand(final String[] command) throws DCRawWrapperException {

        final Process process;
        try {
            process = Runtime.getRuntime().exec(command);
        } catch (final IOException e) {
            throw new DCRawWrapperException("IO Error executing system command: '" + command[0] + "'.", e);
        }

        final StreamGrabber errorStreamGrabber = new StreamGrabber(process.getErrorStream(), "DCRAW: ");
        final StreamGrabber outputStreamGrabber = new StreamGrabber(process.getInputStream(), "DCRAW: ");

        try {

            errorStreamGrabber.start();
            outputStreamGrabber.start();

            int r = process.waitFor();
            if (r == 0) {
                // Wait for outputStreamGrabber to complete
                outputStreamGrabber.join();
            } else {
                final StringBuffer message = new StringBuffer();
                message.append("Lookup thread terminated with code ").append(r).append(".");
                final String errorOutput = errorStreamGrabber.getData().trim();
                if (errorOutput.length() > 0) {
                    message.append('\n').append(errorOutput);
                }
                throw new DCRawWrapperException(message.toString());
            }
        } catch (final InterruptedException e) {
            final StringBuffer message = new StringBuffer("Thread Error executing system command.");
            final String errorOutput = errorStreamGrabber.getData().trim();
            if (errorOutput.length() > 0) {
                message.append('\n').append(errorOutput);
            }
            throw new DCRawWrapperException(message.toString(), e);
        }


        return outputStreamGrabber.getData();
    }

    private static class DCRawWrapperException extends Exception {
        public DCRawWrapperException() {
        }

        public DCRawWrapperException(String message) {
            super(message);
        }

        public DCRawWrapperException(String message, Throwable cause) {
            super(message, cause);
        }

        public DCRawWrapperException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Utility class for grabbing process outputs.
     */
    private static class StreamGrabber extends Thread {
        final private InputStream inputStream;
        final private StringBuffer data = new StringBuffer();
        final private String statusPrefix;

        public StreamGrabber(final InputStream inputStream, final String statusPrefix) {
            this.inputStream = inputStream;
            this.statusPrefix = statusPrefix;
        }

        public void run() {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    data.append(line).append('\n');
                    final String message = statusPrefix + line;
                    IJ.showStatus(message);
                    log(message);
                }
                reader.close();
            } catch (final IOException exception) {
                exception.printStackTrace();
            }
        }

        public String getData() {
            return data.toString();
        }
    }
}
