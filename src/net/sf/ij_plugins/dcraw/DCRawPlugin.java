/*
 * Image/J Plugins
 * Copyright (C) 2002-2013 Jarek Sacha
 * Author's email: jsacha at users dot sourceforge dot net
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
import ij.Prefs;
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


/**
 * Plugin for opening RAW images. It calls DCRAW to convert a RAW image to PPM then loads that PPM image.
 * <p/>
 * The home site for DCRAW is http://www.cybercom.net/~dcoffin/dcraw/.
 *
 * @author Jarek Sacha
 */
public class DCRawPlugin implements PlugIn {

    private static final String TITLE = "DCRaw Reader";
    private static final String ABOUT = "" +
            "The Digital Camera Raw Reader plugin opens raw image formats from over 500 cameras using\n" +
            "DCRAW program created by Dave Coffin. Full list of supported cameras can be\n" +
            "found at DCRAW home page: http://www.cybercom.net/~dcoffin/dcraw/\n" +
            "---\n" +
            "The DCRaw Reader plugin requires the DCRAW binary. Versions for various operating\n" +
            "systems can be downloaded through the Reader home page:\n" +
            "http://ij-plugins.sourceforge.net/plugins/dcraw/ or through DCRAW home page.\n" +
            "---\n" +
            "By default, the DCRaw Reader plugin looks for the DCRAW binary in subdirectory\n" +
            "'dcraw' of ImageJ plugins folder. Alternative location can be specified by adding\n" +
            "property '" + Prefs.KEY_PREFIX + DCRawReader.SYSTEM_PROPERTY_DCRAW_BIN
            + "' to ImageJ properties file IJ_Props.txt located in\n" +
            "ImageJ home directory. Example line that should be added to IJ_Props.txt:\n" +
            Prefs.KEY_PREFIX + DCRawReader.SYSTEM_PROPERTY_DCRAW_BIN + "=/apps/bin/dcraw.exe\n" +
            "Reading of 48 bit RGB images requires ImageJ v.1.35p or newer.";
    //    private static boolean useTmpDir = true;
    private static final Config CONFIG = new Config();

    private static void log(final String message) {
        if (IJ.debugMode) {
            IJ.log(message);
        }
    }

    private static String toProcessedFileName(final String rawFileName) {
        final String processedExtension = ".tiff";
        final int dotIndex = rawFileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return rawFileName + processedExtension;
        } else {
            return rawFileName.substring(0, dotIndex) + processedExtension;
        }
    }

    private static void copyFile(final File sourceFile, final File destFile) throws IOException {
        if (!destFile.exists()) {
            if (!destFile.createNewFile()) {
                throw new IOException("Destination file cannot be created: " + destFile.getPath());
            }
        }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    @Override
    public void run(final String arg) {

        final String title = TITLE + " (v." + DCRawVersion.getInstance() + ")";

        if ("about".equalsIgnoreCase(arg)) {
            IJ.showMessage("About " + title, ABOUT);
            return;
        }

        final DCRawReader dcRawReader = new DCRawReader();
        dcRawReader.addLogListener(new DCRawReader.LogListener() {
            @Override
            public void log(String message) {
                DCRawPlugin.log(message);
            }
        });
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

            dialog.addCheckbox("Use_temporary_directory for processing", CONFIG.useTmpDir);

            // Auto white balance
            dialog.addChoice("White_balance", Config.whiteBalanceChoice[0], Config.whiteBalanceChoice[0][CONFIG.whiteBalance]);

            dialog.addCheckbox("Do_not_automatically_brighten the image", CONFIG.doNotAutomaticallyBrightenTheImage);

            // -o [0-5]  Output colorspace (raw,sRGB,Adobe,Wide,ProPhoto,XYZ)
            dialog.addChoice("Output_colorspace", Config.outputColorSpaceChoice[0], Config.outputColorSpaceChoice[0][CONFIG.outputColorSpace]);

            // -d        Document mode (no color, no interpolation)
            dialog.addCheckbox("Document_mode (no color, no interpolation)", CONFIG.documentMode);

            // -D        Document mode without scaling (totally raw)
            dialog.addCheckbox("Document_mode_without_scaling (totally raw)", CONFIG.documentModeWithoutScaling);

            // Image bit format
            dialog.addChoice("Read_as", Config.formatChoice[0], Config.formatChoice[0][CONFIG.format]);

            // Interpolation quality
            dialog.addChoice("Interpolation quality", Config.interpolationQualityChoice[0], Config.interpolationQualityChoice[0][CONFIG.interpolationQuality]);

            dialog.addCheckbox("Half_size", CONFIG.halfSize);

            dialog.addCheckbox("Do_not_rotate or scale pixels (preserve orientation and aspect ratio)", CONFIG.doNotRotate);

            dialog.addCheckbox("Show_metadata in Result window", CONFIG.showMetadata);

            dialog.addHelp("http://ij-plugins.sourceforge.net/plugins/dcraw/");

            //
            // Show dialog
            //
            dialog.showDialog();

            if (dialog.wasCanceled()) {
                // No selection
                return;
            }

            CONFIG.useTmpDir = dialog.getNextBoolean();
            CONFIG.whiteBalance = dialog.getNextChoiceIndex();
            CONFIG.doNotAutomaticallyBrightenTheImage = dialog.getNextBoolean();
            CONFIG.outputColorSpace = dialog.getNextChoiceIndex();
            CONFIG.documentMode = dialog.getNextBoolean();
            CONFIG.documentModeWithoutScaling = dialog.getNextBoolean();
            CONFIG.format = dialog.getNextChoiceIndex();
            CONFIG.interpolationQuality = dialog.getNextChoiceIndex();
            CONFIG.halfSize = dialog.getNextBoolean();
            CONFIG.doNotRotate = dialog.getNextBoolean();
            CONFIG.showMetadata = dialog.getNextBoolean();

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

            // Check if PPM file existed before it will be written created by DCRAW
            processedFile = new File(actualInput.getParentFile(), toProcessedFileName(actualInput.getName()));
            removeProcessed = !processedFile.exists();


            //
            // Convert user choices to command line options
            //

            // Command line components
            final List<String> commandList = new ArrayList<String>();

            // Turn on verbose messages
            commandList.add("-v");

            // Convert images to TIFF (otherwise DCRAW may produce PPM or PGM depending on processing)
            commandList.add("-T");

            // White balance
            commandList.add(Config.whiteBalanceChoice[1][CONFIG.whiteBalance]);

            // Brightness adjustment
            if (CONFIG.doNotAutomaticallyBrightenTheImage) {
                commandList.add("-W");
            }

            // Colorspace
            commandList.add("-o");
            commandList.add(Config.outputColorSpaceChoice[1][CONFIG.outputColorSpace]);

            // -d Document mode (no color, no interpolation)
            if (CONFIG.documentMode) {
                commandList.add("-d");
            }

            // -D Document mode without scaling (totally raw)
            if (CONFIG.documentModeWithoutScaling) {
                commandList.add("-D");
            }

            // Image bit format
            commandList.add(Config.formatChoice[1][CONFIG.format]);

            // Interpolation quality
            commandList.add("-q");
            commandList.add(Config.interpolationQualityChoice[1][CONFIG.interpolationQuality]);

            // Extract at half size
            if (CONFIG.halfSize) {
                commandList.add("-h");
            }

            // Do not rotate or correct pixel aspect ratio
            if (CONFIG.doNotRotate) {
                commandList.add("-j");
            }

            final boolean showMatadata = CONFIG.showMetadata;

            // Add input raw file
            commandList.add(actualInput.getAbsolutePath());

            //
            // Run DCRAW
            //
            final String[] command = commandList.toArray(new String[commandList.size()]);
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
                IJ.error("Unable to locate DCRAW output PPM file: '" + processedFile.getAbsolutePath() + "'.");
                return;
            }
            IJ.showStatus("Opening: " + processedFile.getAbsolutePath());
            IJ.open(processedFile.getAbsolutePath());

            // Use DCRaw to extract metadata
            if (showMatadata) {
                final String[] metadataCommand = new String[]{"-i", "-v", rawFile.getAbsolutePath()};
                final String metadataOutput;
                try {
                    metadataOutput = dcRawReader.executeCommand(metadataCommand);
                } catch (DCRawException e) {
                    e.printStackTrace();
                    IJ.error(title, e.getMessage());
                    IJ.showMessage("About " + title, ABOUT);
                    return;
                }
                IJ.log(metadataOutput);
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
        private static final String[][] whiteBalanceChoice = {
                {"None", "Camera white balance", "Averaging the entire image"},
                {"", "-w", "-a"}
        };
        private static final String[][] outputColorSpaceChoice = {{
                "0 - raw",
                "1 - sRGB",
                "2 - Adobe",
                "3 - Wide",
                "4 - ProPhoto",
                "5 - XYZ"},
                {"0", "1", "2", "3", "4", "5"}
        };
        private static final String[][] formatChoice = {
                {"8-bit", "16-bit", "16-bit linear"},
                {"", "-6", "-4"}
        };
        private static final String[][] interpolationQualityChoice = {{
                "0 - High-speed, low-quality bilinear",
                "1 - Variable Number of Gradients (VNG)",
                "2 - Patterned Pixel Grouping (PPG)",
                "3 - Adaptive Homogeneity-Directed (AHD)"},
                {"0", "1", "2", "3"}
        };
        public int whiteBalance = 1;
        public boolean doNotAutomaticallyBrightenTheImage;
        public int outputColorSpace = 0;
        public boolean documentMode;
        public boolean documentModeWithoutScaling;
        public int format = 0;
        public int interpolationQuality = 0;
        public boolean halfSize;
        public boolean doNotRotate;
        public boolean showMetadata;
        boolean useTmpDir = true;
    }
}
