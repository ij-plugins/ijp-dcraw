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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
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

    private static final String TITLE = "Open RAW image";
    private String dcrawFileName = "/usr/bin/dcraw";


    public void run(final String arg) {

        // Establish location of DCRAW executable
        final String dcrawPath = ij.Menus.getPlugInsPath() + File.separator + dcrawFileName;
        final File dcrawFile = new File(dcrawPath);
        if (!dcrawFile.exists()) {
            IJ.error("Invalid path to DCRAW executable: '" + dcrawFile.getAbsolutePath() + "'.");
            return;
        }

        // Ask for location of the RAW file to read
        final OpenDialog openDialog = new OpenDialog(TITLE, null);
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
        final GenericDialog dialog = new GenericDialog("'" + TITLE + "' Options");

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
        commandList.add(dcrawPath);
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
        if (IJ.debugMode) {
            IJ.log("DCRAW command line elements:");
            for (int i = 0; i < command.length; i++) {
                IJ.log("  " + command[i]);
            }
        }
        try {
            executeCommand(command);
        } catch (DCRawWrapperException e) {
            e.printStackTrace();
            IJ.error(e.getMessage());
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
            IJ.error(TITLE, e.getMessage());
            return;
        }

        final ImagePlus imp;
        imp = new ImagePlus(ppmFile.getName(), stack);
        imp.show();

        // Remove PPM if it did not exist
        if (removePPM) {
//            ppmFile.delete();
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

    private static String executeCommand(final String[] command) throws DCRawWrapperException {

        final Process process;
        try {
            process = Runtime.getRuntime().exec(command);
        } catch (final IOException e) {
            throw new DCRawWrapperException("IO Error executing system command: '" + command[0] + "'.", e);
        }

        final StreamGrabber errorStreamGrabber = new StreamGrabber(process.getErrorStream(), "DCRAW: ");
        final StreamGrabber outputStreamGrabber = new StreamGrabber(process.getInputStream(), "dcraw: ");

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
                String line = null;
                while ((line = reader.readLine()) != null) {
                    data.append(line).append('\n');
                    IJ.showStatus(statusPrefix + line);
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
