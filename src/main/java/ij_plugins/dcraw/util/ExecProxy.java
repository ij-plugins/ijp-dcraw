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

package ij_plugins.dcraw.util;

import ij.IJ;
import ij.Menus;
import ij.Prefs;
import ij_plugins.dcraw.DCRawException;
import ij_plugins.dcraw.LogListener;

import java.io.*;
import java.util.Arrays;
import java.util.Optional;


/**
 * Helps to execute an external process
 *
 * @author Jarek Sacha
 */
public final class ExecProxy {

    /**
     * Name of the system property that can contain path to the executable
     */
    public final String systemPropertyExecPath;
    private final String executableNameCore; // dcraw_emu
    private final Optional<LogListener> statusLogListener;
    private final Optional<LogListener> errorLogListener;
    private String executablePath = null;

    public ExecProxy(final String executableNameCore,
                     final String systemPropertyExecPath,
                     final Optional<LogListener> statusLogListener,
                     final Optional<LogListener> errorLogListener) {
        this.executableNameCore = executableNameCore;
        this.systemPropertyExecPath = systemPropertyExecPath;
        this.statusLogListener = statusLogListener;
        this.errorLogListener = errorLogListener;
    }

    private String executableName() {
        return IJ.isWindows() ? executableNameCore + ".exe" : executableNameCore;
    }

    public void validateExecutable() throws DCRawException {
        if (executablePath == null) {
            executablePath = locateExecutable();
        }
    }

    /**
     * Attempt to locate the executable. Search locations:
     * <ul>
     * <li>Full path as specified by system property <tt>systemPropertyExecPath</tt> </li>
     * <li>Full path as specified by ImageJ property <tt>systemPropertyExecPath</tt> (can be set in IJ_Prefs.txt)</li>
     * <li>Inside {@code dcraw} subdirectory of ImageJ plugins directory.</li>
     * <li>System path. In this case simply file name rather than full path will be returned.</li>
     * </ul>
     *
     * @return location of DCRAW executable.
     * @throws DCRawException if DCRAW cannot be found.
     */
    private String locateExecutable() throws DCRawException {
        final String systemDCRawExecutablePath = System.getProperty(systemPropertyExecPath, null);
        final String exeRelativeToPluginsDir = "dcraw" + File.separator + executableName();
        final File devEnvLocation = new File("plugins" + File.separator + exeRelativeToPluginsDir);

        final String execPath;
        if (systemDCRawExecutablePath != null) {
            // Try to read from a system property
            final File file = new File(systemDCRawExecutablePath);
            if (!file.exists() || file.isDirectory()) {
                throw new DCRawException("System property '" + systemPropertyExecPath +
                        "' does not point to an existing '" + executableNameCore +
                        "' executable [" + file.getAbsolutePath() + "]");
            }
            execPath = file.getAbsolutePath();
        } else if (Prefs.get(systemPropertyExecPath, null) != null) {
            // Try to read from ImageJ properties
            final String path = Prefs.get(systemPropertyExecPath, null);
            final File file = new File(path);
            if (!file.exists()) {
                throw new DCRawException("ImageJ property '" + systemPropertyExecPath +
                        "' (IJ_Prefs.txt) does not point to an existing '" + executableNameCore +
                        "' executable [" + file.getAbsolutePath() + "]");
            }
            execPath = new File(path).getAbsolutePath();
        } else if (Menus.getPlugInsPath() != null) {
            // Try to locate in ImageJ plugins directory
            final String path = Menus.getPlugInsPath() + File.separator + exeRelativeToPluginsDir;
            final File file = new File(path);
            if (!file.exists()) {
                throw new DCRawException("Unable to find '" + executableNameCore +
                        "' binary in ImageJ plugins folder. File does not exist: '" + file.getAbsolutePath() + "'.");
            }
            execPath = new File(path).getAbsolutePath();
        } else if (devEnvLocation.exists()) {
            // This branch is intended primarily for execution in test environment
            execPath = devEnvLocation.getAbsolutePath();
        } else {
            // Attempt to use system path
            execPath = executableName();
            try {
                executeCommandImpl(new String[]{}, execPath);
            } catch (DCRawException ex) {
                throw new DCRawException("Failed to find '" + executableName() + "' binary in the system path.", ex);
            }
        }

        return execPath;
    }

    public Result executeCommand(final String[] command) throws DCRawException {
        if (executablePath == null) {
            executablePath = locateExecutable();
        }

        return executeCommandImpl(command, executablePath);
    }

    private Result executeCommandImpl(final String[] command, String execPath) throws DCRawException {
        if (execPath == null) {
            throw new DCRawException("Cannot run, location of " + executableName() + " not known.");
        }

        final String[] fullCommand = new String[command.length + 1];
        fullCommand[0] = execPath;
        System.arraycopy(command, 0, fullCommand, 1, command.length);

        final Process process;
        statusLogListener.ifPresent(l -> l.log("Executing command array: " + Arrays.toString(fullCommand)));

        try {
            process = Runtime.getRuntime().exec(fullCommand);
        } catch (final IOException e) {
            throw new DCRawException("IO Error executing system command: '" + fullCommand[0] + "'.", e);
        }

        final StreamGrabber errorStreamGrabber =
                new StreamGrabber(process.getErrorStream(), executableNameCore + ": ", errorLogListener);
        final StreamGrabber outputStreamGrabber =
                new StreamGrabber(process.getInputStream(), executableNameCore + ": ", statusLogListener);

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

            errorStreamGrabber.join();
            outputStreamGrabber.join();
        } catch (final InterruptedException e) {
            final StringBuilder message = new StringBuilder("Thread Error executing system command.");
            final String errorOutput = errorStreamGrabber.getData().trim();
            if (errorOutput.length() > 0) {
                message.append('\n').append(errorOutput);
            }
            throw new DCRawException(message.toString(), e);
        }


        return new Result(outputStreamGrabber.getData(), errorStreamGrabber.getData());
    }

    public static class Result {
        public final String stdOut;
        public final String stdErr;

        public Result(String stdOut, String stdErr) {
            this.stdOut = stdOut;
            this.stdErr = stdErr;
        }
    }

    /**
     * Utility class for grabbing process outputs.
     */
    private static class StreamGrabber extends Thread {

        final Optional<LogListener> logListener;
        private final InputStream inputStream;
        private final StringBuffer data = new StringBuffer();
        private final String statusPrefix;


        public StreamGrabber(final InputStream inputStream, final String statusPrefix, final Optional<LogListener> logListener) {
            this.inputStream = inputStream;
            this.statusPrefix = statusPrefix;
            this.logListener = logListener;
        }

        @Override
        public void run() {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    data.append(line).append('\n');
                    final String message = statusPrefix + line;
                    logListener.ifPresent(listener -> listener.log(message));
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
