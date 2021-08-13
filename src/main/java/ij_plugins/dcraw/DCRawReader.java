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
import ij.Menus;
import ij.Prefs;

import java.io.*;
import java.util.Arrays;
import java.util.Vector;


/**
 * Reads digital camera raw files using DCRAW executable.
 * More info on DCRAW at {@code http://www.cybercom.net/%7Edcoffin/dcraw/}.
 *
 * @author Jarek Sacha
 */
public final class DCRawReader {

    public static final String SYSTEM_PROPERTY_DCRAW_BIN = "dcrawExecutable.path";
    private final Vector<LogListener> listeners = new Vector<LogListener>();
    private String dcrawBinPath;

    private static String dcrawExecutableName() {
        return IJ.isWindows() ? "dcraw/dcraw.exe" : "dcraw/dcraw";
    }

    public void addLogListener(final LogListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeLogListener(final LogListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void removeAllLogListeners() {
        listeners.clear();
    }

    public void validateDCRaw() throws DCRawException {
        if (dcrawBinPath == null) {
            dcrawBinPath = locateDCRAW();
        }
    }

    /**
     * Attempt to locate DCRAW executable. Search locations locations:
     * <ul>
     * <li>Full path as specified by system property <tt>{@value #SYSTEM_PROPERTY_DCRAW_BIN}</tt> </li>
     * <li>Full path as specified by ImageJ property <tt>{@value #SYSTEM_PROPERTY_DCRAW_BIN}</tt> (can be set in IJ_Prefs.txt)</li>
     * <li>Inside {@code dcraw} subdirectory of ImageJ plugins directory.</li>
     * <li>System path. In this case simply file name rather than full path will be returned.</li>
     * </ul>
     *
     * @return location of DCRAW executable.
     * @throws DCRawException if DCRAW cannot be found.
     */
    private String locateDCRAW() throws DCRawException {
        final String systemDCRawExecutablePath = System.getProperty(SYSTEM_PROPERTY_DCRAW_BIN, null);
        if (systemDCRawExecutablePath != null) {
            // Try to read from a system property
            final File file = new File(systemDCRawExecutablePath);
            if (!file.exists() || file.isDirectory()) {
                throw new DCRawException("System property '" + SYSTEM_PROPERTY_DCRAW_BIN
                        + "' does not point to an existing DCRAW executable ["
                        + file.getAbsolutePath() + "]");
            }
            dcrawBinPath = file.getAbsolutePath();
        } else if (Prefs.get(SYSTEM_PROPERTY_DCRAW_BIN, null) != null) {
            // Try to read from ImageJ properties
            final String path = Prefs.get(SYSTEM_PROPERTY_DCRAW_BIN, null);
            final File file = new File(path);
            if (!file.exists()) {
                throw new DCRawException("ImageJ property '" + SYSTEM_PROPERTY_DCRAW_BIN
                        + "' (IJ_Prefs.txt) does not point to an existing DCRAW executable ["
                        + file.getAbsolutePath() + "]");
            }
            dcrawBinPath = new File(path).getAbsolutePath();
        } else if (Menus.getPlugInsPath() != null) {
            // Try to locate in ImageJ plugins directory
            final String path = Menus.getPlugInsPath() + File.separator + dcrawExecutableName();
            final File file = new File(path);
            if (!file.exists()) {
                throw new DCRawException(
                        "Unable to find DCRAW binary in ImageJ plugins folder. File does not exist: '"
                                + file.getAbsolutePath() + "'.");
            }
            dcrawBinPath = new File(path).getAbsolutePath();
        } else {
            // Attempt to use system path
            dcrawBinPath = dcrawExecutableName();
            try {
                executeCommand(new String[]{dcrawBinPath});
            } catch (DCRawException ex) {
                throw new DCRawException("Failed to find DCRAW binary in system path.", ex);
            }
        }

        return dcrawBinPath;
    }

    public String executeCommand(final String[] command) throws DCRawException {
        validateDCRaw();

        final String[] fullCommand = new String[command.length + 1];
        fullCommand[0] = dcrawBinPath;
        System.arraycopy(command, 0, fullCommand, 1, command.length);

        final Process process;
        log("Executing command array: " + Arrays.toString(fullCommand));

        // Disable CygWin warning about DOS path names (if running CygWin compiled dcraw)
        final String envp[] = {"CYGWIN=nodosfilewarning"};
        try {
            process = Runtime.getRuntime().exec(fullCommand, envp);
        } catch (final IOException e) {
            throw new DCRawException("IO Error executing system command: '" + command[0] + "'.", e);
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
                final StringBuilder message = new StringBuilder();
                message.append("Lookup thread terminated with code ").append(r).append(".");
                final String errorOutput = errorStreamGrabber.getData().trim();
                if (errorOutput.length() > 0) {
                    message.append('\n').append(errorOutput);
                }
                throw new DCRawException(message.toString());
            }
        } catch (final InterruptedException e) {
            final StringBuilder message = new StringBuilder("Thread Error executing system command.");
            final String errorOutput = errorStreamGrabber.getData().trim();
            if (errorOutput.length() > 0) {
                message.append('\n').append(errorOutput);
            }
            throw new DCRawException(message.toString(), e);
        }


        return outputStreamGrabber.getData();
    }

    private void log(final String message) {
        for (LogListener listener : listeners) {
            listener.log(message);
        }
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
        XYZ("XYZ", "5");
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
        AHD("Adaptive Homogeneity-Directed (AHD)", "3");
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


    public static interface LogListener {

        void log(String message);
    }

    /**
     * Utility class for grabbing process outputs.
     */
    private class StreamGrabber extends Thread {

        private final InputStream inputStream;
        private final StringBuffer data = new StringBuffer();
        private final String statusPrefix;


        public StreamGrabber(final InputStream inputStream, final String statusPrefix) {
            this.inputStream = inputStream;
            this.statusPrefix = statusPrefix;
        }

        @Override
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
