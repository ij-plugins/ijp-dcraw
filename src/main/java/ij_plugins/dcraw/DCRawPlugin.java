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
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import static ij_plugins.dcraw.DCRawReader.*;


/**
 * Plugin for opening RAW images. It calls DCRAW to convert a RAW image to PPM then loads that PPM image.
 * <br>
 * The home site for DCRAW is http://www.cybercom.net/~dcoffin/dcraw/.
 *
 * @author Jarek Sacha
 */
public class DCRawPlugin implements PlugIn {

    public static final String TITLE = "DCRaw Reader";
    public static final String HELP_URL = "https://github.com/ij-plugins/ijp-dcraw";

    private static final String HTML_DESCRIPTION = "Open image file in a camera raw format using the \"dcraw\" tool.";


    private static final String ABOUT = "<html>" +
            "<p>" +
            "\"DCRaw Reader\" plugin open image file in a camera raw format using the \"dcraw\" tool created by Dave Coffin." +
            "</p>" +
            "<p>" +
            "For more information about \"DCRaw Reader\" see project page at  <a href=\"" + HELP_URL + "\">" + HELP_URL + "</a> " +
            "</p>" +
            "</html>";
    //    private static boolean useTmpDir = true;
    private static final Config CONFIG = new Config();

    private static void log(final String message) {
        if (IJ.debugMode) {
            IJ.log(message);
        }
    }

    private static String toProcessedFileName(final String rawFileName) {
        return rawFileName + ".tiff";
    }

    private static void copyFile(final File sourceFile, final File destFile) throws IOException {
        if (!destFile.exists()) {
            if (!destFile.createNewFile()) {
                throw new IOException("Destination file cannot be created: " + destFile.getPath());
            }
        }

        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
             FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
    }

    private static <T> String[] asStrings(T[] v) {
        final String[] r = new String[v.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = v[i].toString();
        }
        return r;
    }

    @Override
    public void run(final String arg) {

        final String title = TITLE + " (v." + DCRawVersion.getInstance() + ")";

        if ("about".equalsIgnoreCase(arg)) {
            IJ.showMessage("About " + title, ABOUT);
            return;
        }

        final DCRawReader dcRawReader = new DCRawReader();
        dcRawReader.addLogListener(DCRawPlugin::log);
        File processedFile = null;
        boolean removeProcessed = false;
        File actualInput = null;
        try {
            // Verify that could talk to DCRAW
            try {
                dcRawReader.validateDCRaw();
            } catch (DCRawException e) {
                e.printStackTrace();
                IJ.error(title, e.getMessage());
                IJ.showMessage("About " + title, ABOUT);
                return;
            }

            // Ask for location of the RAW file to read
            final OpenDialog openDialog = new OpenDialog("Open", null);
            if (openDialog.getFileName() == null) {
                // No selection
                return;
            }

            final File rawFile = new File(openDialog.getDirectory(), openDialog.getFileName());
            IJ.showStatus("Opening RAW file: " + rawFile.getName());


            //
            // Setup options dialog
            //
            final GenericDialog dialog = new GenericDialog(title);
            dialog.setIconImage(IJPUtils.imageJIconAsAWTImage());

            dialog.addPanel(IJPUtils.createInfoPanel(TITLE, HTML_DESCRIPTION));

            dialog.addCheckbox("Use_temporary_directory for processing", CONFIG.useTmpDir);

            // Auto white balance
            dialog.addChoice("White_balance", asStrings(WhiteBalanceOption.values()),
                    CONFIG.whiteBalance.toString());

            dialog.addCheckbox("Do_not_automatically_brighten the image",
                    CONFIG.doNotAutomaticallyBrightenTheImage);

            // -o [0-5]  Output colorspace (raw,sRGB,Adobe,Wide,ProPhoto,XYZ)
            dialog.addChoice("Output_colorspace", asStrings(OutputColorSpaceOption.values()),
                    CONFIG.outputColorSpace.toString());

            // Image bit format
            dialog.addChoice("Read_as", asStrings(FormatOption.values()), CONFIG.format.toString());

            // Interpolation quality
            dialog.addChoice("Interpolation quality", asStrings(InterpolationQualityOption.values()),
                    CONFIG.interpolationQuality.toString());

            dialog.addCheckbox("Half_size", CONFIG.halfSize);

            dialog.addCheckbox("Do_not_rotate or scale pixels (preserve orientation and aspect ratio)",
                    CONFIG.doNotRotate);

            dialog.addHelp(HELP_URL);

            //
            // Show dialog
            //
            dialog.showDialog();

            if (dialog.wasCanceled()) {
                // No selection
                return;
            }

            CONFIG.useTmpDir = dialog.getNextBoolean();
            CONFIG.whiteBalance = WhiteBalanceOption.byName(dialog.getNextChoice());
            CONFIG.doNotAutomaticallyBrightenTheImage = dialog.getNextBoolean();
            CONFIG.outputColorSpace = OutputColorSpaceOption.byName(dialog.getNextChoice());
            CONFIG.format = FormatOption.byName(dialog.getNextChoice());
            CONFIG.interpolationQuality = InterpolationQualityOption.byName(dialog.getNextChoice());
            CONFIG.halfSize = dialog.getNextBoolean();
            CONFIG.doNotRotate = dialog.getNextBoolean();

            if (CONFIG.useTmpDir) {
                // Copy file to a temp file to avoid overwriting data ast the source
                // DCRAW always writes output in the same directory as the input file.
                try {
                    actualInput = File.createTempFile("dcraw_", "_" + rawFile.getName());
                    actualInput.deleteOnExit();
                } catch (final IOException e) {
                    e.printStackTrace();
                    IJ.error(title, "Failed to create temporary file for processing. " + e.getMessage());
                    return;
                }

                {
                    final String m = "Copying input to " + actualInput.getAbsolutePath();
                    IJ.showStatus(m);
                    log(m);
                }
                try {
                    copyFile(rawFile, actualInput);
                } catch (final IOException e) {
                    e.printStackTrace();
                    IJ.error(title, "Failed to copy image to a temporary file for processing. " + e.getMessage());
                    return;
                }
            } else {
                actualInput = rawFile;
            }

            // Check if TIFF file existed before it will be written created by DCRAW
            processedFile = new File(actualInput.getParentFile(), toProcessedFileName(actualInput.getName()));
            removeProcessed = !processedFile.exists();


            //
            // Convert user choices to command line options
            //

            // Command line components
            final List<String> commandList = new ArrayList<>();

            // Turn on verbose messages
            commandList.add("-v");

            // Convert images to TIFF (otherwise DCRAW may produce PPM or PGM depending on processing)
            commandList.add("-T");

            // White balance
            if (!CONFIG.whiteBalance.getOption().trim().isEmpty()) {
                commandList.add(CONFIG.whiteBalance.getOption());
            }

            // Brightness adjustment
            if (CONFIG.doNotAutomaticallyBrightenTheImage) {
                commandList.add("-W");
            }

            // Colorspace
            commandList.add("-o");
            commandList.add(CONFIG.outputColorSpace.getOption());

            // Image bit format
            if (!CONFIG.format.getOption().trim().isEmpty()) {
                commandList.add(CONFIG.format.getOption());
            }

            // Interpolation quality
            commandList.add("-q");
            commandList.add(CONFIG.interpolationQuality.getOption());

            // Extract at half size
            if (CONFIG.halfSize) {
                commandList.add("-h");
            }

            // Do not rotate or correct pixel aspect ratio
            if (CONFIG.doNotRotate) {
                commandList.add("-j");
            }

            // Add input raw file
            commandList.add(actualInput.getAbsolutePath());

            //
            // Run DCRAW
            //
            final String[] command = commandList.toArray(new String[0]);
            try {
                dcRawReader.executeCommand(command);
            } catch (DCRawException e) {
                e.printStackTrace();
                IJ.error(title, e.getMessage());
                IJ.showMessage("About " + title, ABOUT);
                return;
            }

            // Read PPM file
            if (!processedFile.exists()) {
                IJ.error("Unable to locate DCRAW output TIFF file: '" + processedFile.getAbsolutePath() + "'.");
                return;
            }
            IJ.showStatus("Opening: " + processedFile.getAbsolutePath());
            final ImagePlus imp = IJ.openImage(processedFile.getAbsolutePath());
            if (imp == null) {
                IJ.error(TITLE, "Failed to open converted image file: " + processedFile.getAbsolutePath());
            } else {
                // Set image name, default name may contain temporary file name used during conversion
                imp.setTitle(rawFile.getName());
                imp.show();
            }
        } finally {
            //
            // Cleanup
            //
            dcRawReader.removeAllLogListeners();
            // Remove processed file if needed
            if ((CONFIG.useTmpDir || removeProcessed) && processedFile != null && processedFile.exists()) {
                if (!processedFile.delete()) {
                    IJ.error(title, "Failed to delete the processed file: " + processedFile.getAbsolutePath());
                }
            }
            // Remove temporary copy of the raw file
            if (CONFIG.useTmpDir && actualInput != null && actualInput.exists()) {
                if (!actualInput.delete()) {
                    IJ.error(title, "Failed to delete temporary copy of the raw file: " + actualInput.getAbsolutePath());
                }
            }
        }

    }

    private static class Config {
        public WhiteBalanceOption whiteBalance = WhiteBalanceOption.CAMERA;
        public boolean doNotAutomaticallyBrightenTheImage;
        public OutputColorSpaceOption outputColorSpace = OutputColorSpaceOption.RAW;
        public FormatOption format = FormatOption.F_8_BIT;
        public InterpolationQualityOption interpolationQuality = InterpolationQualityOption.DHT;
        public boolean halfSize;
        public boolean doNotRotate;
        boolean useTmpDir = true;
    }
}
