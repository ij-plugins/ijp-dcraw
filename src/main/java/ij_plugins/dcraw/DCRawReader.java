/*
 * IJ-Plugins
 * Copyright (C) 2002-2022 Jarek Sacha
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
import ij_plugins.dcraw.util.ExecProxy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Reads digital camera raw files using DCRAW executable.
 * More info on DCRAW at <a href="http://www.cybercom.net/%7Edcoffin/dcraw/">http://www.cybercom.net/%7Edcoffin/dcraw/</a>.
 *
 * @author Jarek Sacha
 */
public final class DCRawReader {

    /**
     * Raw image processing options.
     */
    public static class Config {
        /**
         * White balance to use: none, camera, averaging (-w, -a)
         */
        public WhiteBalanceOption whiteBalance = WhiteBalanceOption.CAMERA;
        /**
         * Do not automatically brighten the image (-W)
         */
        public boolean doNotAutomaticallyBrightenTheImage = true;
        /**
         * Output colorspace: raw, sRGB, Adobe, Wide, ProPhoto, XYZ, ACES  (-o [0-6])
         */
        public OutputColorSpaceOption outputColorSpace = OutputColorSpaceOption.RAW;
        /**
         * Output format, per channel: 8-bit, 16-bit, 16-bit linear
         */
        public FormatOption format = FormatOption.F_8_BIT;

        /**
         * Set the interpolation quality: linear, VNG, PPG, AHD, DCB, DHT, AAHD (-q N)
         */
        public InterpolationQualityOption interpolationQuality = InterpolationQualityOption.DHT;

        /**
         * Half-size color image (twice as fast as "interpolation quality: linear")
         */
        public boolean halfSize = false;
        /**
         * Don't stretch or rotate raw pixels (-j)
         */
        public boolean doNotStretchOrRotate = true;
        /**
         * Generate temporary image file in the default temporary directory.
         * If {@code false} the file is generated in the same directory as the input image.
         */
        boolean useTmpDir = true;
    }

    private final Optional<LogListener> statusLogListener;
    private final Optional<LogListener> errorLogListener;

    public DCRawReader() {
        this.statusLogListener = Optional.empty();
        this.errorLogListener = Optional.empty();
    }

    /**
     * @param statusLogListener callback used to process status messages while dcraw runs
     * @param errorLogListener  callback used to process error messages
     */
    public DCRawReader(final LogListener statusLogListener,
                       final LogListener errorLogListener) {
        this.statusLogListener = Optional.ofNullable(statusLogListener);
        this.errorLogListener = Optional.ofNullable(errorLogListener);
    }

    /**
     * @param statusLogListener callback used to process status messages while dcraw runs
     * @param errorLogListener  callback used to process error messages
     */
    public DCRawReader(final Optional<LogListener> statusLogListener,
                       final Optional<LogListener> errorLogListener) {
        this.statusLogListener = statusLogListener;
        this.errorLogListener = errorLogListener;
    }

    /**
     * Validate that dcraw executable can be run.
     *
     * @throws DCRawException if the attempt to run dcraw executable fails
     */
    public void validateDCRawExec() throws DCRawException {
        final ExecProxy proxy = createExecProxyCaller();
        // Verify that could talk to DCRAW
        proxy.validateExecutable();
    }

    /**
     * RRead raw image
     *
     * @param rawFile input raw file
     * @return decoded image
     * @throws DCRawException when reading fails.
     */
    public ImagePlus read(final File rawFile) throws DCRawException {
        return read(rawFile, new Config());
    }

    /**
     * Read raw image.
     *
     * @param rawFile input raw file
     * @param config  dcraw processing options
     * @return decoded image
     * @throws DCRawException when reading fails.
     */
    public ImagePlus read(final File rawFile, final Config config) throws DCRawException {

        if (!rawFile.exists()) {
            throw new DCRawException("Input file does not exist: " + rawFile.getAbsolutePath());
        }

        final File actualInput;
        if (config.useTmpDir) {
            // Copy file to a temp file to avoid overwriting data ast the source
            // DCRAW always writes output in the same directory as the input file.
            try {
                actualInput = File.createTempFile("dcraw_", "_" + rawFile.getName());
                actualInput.deleteOnExit();
            } catch (final IOException e) {
                throw new DCRawException("Failed to create temporary file for processing. " + e.getMessage(), e);
            }

            {
                final String m = "Copying input to " + actualInput.getAbsolutePath();
                statusLogListener.ifPresent(l -> l.log(m));
            }

            try {
                copyFile(rawFile, actualInput);
            } catch (final IOException e) {
                throw new DCRawException("Failed to copy image to a temporary file for processing. " + e.getMessage(), e);
            }
        } else {
            actualInput = rawFile;
        }

        // Check if TIFF file existed before it will be written created by DCRAW
        final File processedFile = new File(actualInput.getParentFile(), toProcessedFileName(actualInput.getName()));
        final boolean removeProcessed = !processedFile.exists();

        // Convert user choices to command line options
        final List<String> commandList = buildCommandLine(config, actualInput);

        final ImagePlus dst;
        try {
            runDCRaw(commandList);
            dst = readProcessedImage(processedFile);
        } finally {
            //
            // Cleanup
            //
            // Remove processed file if needed
            if ((config.useTmpDir || removeProcessed) && processedFile.exists()) {
                if (!processedFile.delete()) {
                    errorLogListener.ifPresent(l -> l.log("Failed to delete the processed file: " + processedFile.getAbsolutePath()));
                }
            }
            // Remove temporary copy of the raw file
            if (config.useTmpDir && actualInput.exists()) {
                if (!actualInput.delete()) {
                    errorLogListener.ifPresent(l -> l.log("Failed to delete temporary copy of the raw file: " + actualInput.getAbsolutePath()));
                }
            }
        }

        dst.setTitle(rawFile.getName());

        return dst;
    }

    private ExecProxy createExecProxyCaller() {
        return new ExecProxy(
                "dcraw_emu", "dcrawExecutable.path", statusLogListener, errorLogListener);
    }

    private List<String> buildCommandLine(final Config config, final File actualInput) {
        // Command line components
        final List<String> commandList = new ArrayList<>();

        // Turn on verbose messages
        commandList.add("-v");

        // Convert images to TIFF (otherwise DCRAW may produce PPM or PGM depending on processing)
        commandList.add("-T");

        // White balance
        if (!config.whiteBalance.getOption().trim().isEmpty()) {
            commandList.add(config.whiteBalance.getOption());
        }

        // Brightness adjustment
        if (config.doNotAutomaticallyBrightenTheImage) {
            commandList.add("-W");
        }

        // Colorspace
        commandList.add("-o");
        commandList.add(config.outputColorSpace.getOption());

        // Image bit format
        if (!config.format.getOption().trim().isEmpty()) {
            commandList.add(config.format.getOption());
        }

        // Interpolation quality
        commandList.add("-q");
        commandList.add(config.interpolationQuality.getOption());

        // Extract at half size
        if (config.halfSize) {
            commandList.add("-h");
        }

        // Do not rotate or correct pixel aspect ratio
        if (config.doNotStretchOrRotate) {
            commandList.add("-j");
        }

        // Add input raw file
        commandList.add(actualInput.getAbsolutePath());

        return commandList;
    }

    private void runDCRaw(final List<String> commandList) throws DCRawException {
        //
        // Run DCRAW
        //
        final String[] command = commandList.toArray(new String[0]);

        final ExecProxy proxy = createExecProxyCaller();
        try {
            ExecProxy.Result r = proxy.executeCommand(command);
            if (!IJPUtils.isBlank(r.stdErr)) {
                throw new DCRawException(r.stdErr);
            }
        } catch (DCRawException e) {
            throw new DCRawException("Error running DCRaw backend. " + e.getMessage(), e);
        }
    }

    private ImagePlus readProcessedImage(final File processedFile) throws DCRawException {
        // Read PPM file
        if (!processedFile.exists()) {
            throw new DCRawException("Unable to locate DCRAW output TIFF file: '" + processedFile.getAbsolutePath() + "'.");
        }
        statusLogListener.ifPresent(l -> l.log("Opening: " + processedFile.getAbsolutePath()));
        final ImagePlus imp = IJ.openImage(processedFile.getAbsolutePath());
        if (imp == null) {
            throw new DCRawException("Failed to open converted image file: " + processedFile.getAbsolutePath());
        }

        return imp;
    }

    private static void copyFile(final File sourceFile, final File destFile) throws IOException {
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static String toProcessedFileName(final String rawFileName) {
        return rawFileName + ".tiff";
    }

    public enum WhiteBalanceOption {
        NONE("None", ""),
        CAMERA("Camera white balance", "-w"),
        AVERAGING("Averaging the entire image", "-a");
        private final String name;
        private final String option;


        WhiteBalanceOption(final String name, final String option) {
            this.name = name;
            this.option = option;
        }

        public static WhiteBalanceOption byName(final String name) {
            for (WhiteBalanceOption v : values()) {
                if (v.toString().equals(name)) return v;
            }
            throw new IllegalArgumentException("WhiteBalanceOption has no value with name '" + name + "'.");
        }

        public String getOption() {
            return option;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum OutputColorSpaceOption {
        RAW("raw", "0"),
        SRGB("sRGB", "1"),
        ADOBE("Adobe", "2"),
        WIDE("Wide", "3"),
        PRO_PHOTO("ProPhoto", "4"),
        XYZ("XYZ", "5"),
        ACES("ACES", "6");
        private final String name;
        private final String option;


        OutputColorSpaceOption(final String name, final String option) {
            this.name = name;
            this.option = option;
        }

        public static OutputColorSpaceOption byName(final String name) {
            for (OutputColorSpaceOption v : values()) {
                if (v.toString().equals(name)) return v;
            }
            throw new IllegalArgumentException("OutputColorSpaceOption has no value with name '" + name + "'.");
        }

        public String getOption() {
            return option;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum FormatOption {
        F_8_BIT("8-bit", ""),
        F_16_BIT("16-bit", "-6"),
        F_16_BIT_LINEAR("16-bit linear", "-4");
        private final String name;
        private final String option;


        FormatOption(final String name, final String option) {
            this.name = name;
            this.option = option;
        }

        public static FormatOption byName(final String name) {
            for (FormatOption v : values()) {
                if (v.toString().equals(name)) return v;
            }
            throw new IllegalArgumentException("FormatOption has no value with name '" + name + "'.");
        }

        public String getOption() {
            return option;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum InterpolationQualityOption {
        HIGH_SPEED("High-speed, low-quality bilinear", "0"),
        VNG("Variable Number of Gradients (VNG)", "1"),
        PPG("Patterned Pixel Grouping (PPG)", "2"),
        AHD("Adaptive Homogeneity-Directed (AHD)", "3"),
        DCB("DCB", "4"),
        DHT("DHT", "11"),
        AAHD("Modified AHD (AAHD)", "12");
        private final String name;
        private final String option;


        InterpolationQualityOption(final String name, final String option) {
            this.name = name;
            this.option = option;
        }

        public static InterpolationQualityOption byName(final String name) {
            for (InterpolationQualityOption v : values()) {
                if (v.toString().equals(name)) return v;
            }
            throw new IllegalArgumentException("InterpolationQualityOption has no value with name '" + name + "'.");
        }

        public String getOption() {
            return option;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
