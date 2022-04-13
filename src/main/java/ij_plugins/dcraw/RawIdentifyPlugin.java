/*
 * IJ-Plugins DCRaw
 * Copyright (C) 2008-2022 Jarek Sacha
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
 * Latest release available at https://github.com/ij-plugins/ijp-dcraw
 */

package ij_plugins.dcraw;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij_plugins.dcraw.util.ExecProxy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ij_plugins.dcraw.IJPUtils.isBlank;

public class RawIdentifyPlugin implements PlugIn {

    public static final String TITLE = "Raw Identify";
    public static final String HELP_URL = "https://github.com/ij-plugins/ijp-dcraw";

    private static final String HTML_DESCRIPTION = "Describes raw file type and can print its metadata.";

    private static boolean verboseOutput = false;

    @Override
    public void run(String arg) {

        final String title = TITLE + " (v." + DCRawVersion.getInstance() + ")";

        LogListener statusLogger = message -> {
            IJ.showStatus("DCRaw: " + message);
            if (IJ.debugMode) IJ.log("DCRaw: " + message);
        };
        LogListener errorLogger = message -> {
            IJ.showStatus("ERROR DCRaw: " + message);
            if (IJ.debugMode) IJ.log("ERROR DCRaw: " + message);
        };

        // raw-identify wrapper
        final ExecProxy proxy = new ExecProxy("raw-identify",
                "raw-identifyExecutable.path",
                Optional.of(statusLogger),
                Optional.of(errorLogger));

        // Verify that could talk to DCRAW before asking the user to select options
        try {
            proxy.validateExecutable();
        } catch (DCRawException e) {
            e.printStackTrace();
            errorLogger.log(e.getMessage());
            return;
        }

        // Ask for location of the RAW file to read
        final OpenDialog openDialog = new OpenDialog("Open", null);
        if (openDialog.getFileName() == null) {
            // No selection
            return;
        }

        final File rawFile = new File(openDialog.getDirectory(), openDialog.getFileName());
        IJ.showStatus("Identifying RAW file: " + rawFile.getName());

        final GenericDialog dialog = new GenericDialog(title);
        dialog.setIconImage(IJPUtils.imageJIconAsAWTImage());
        dialog.addPanel(IJPUtils.createInfoPanel(TITLE, HTML_DESCRIPTION));
        dialog.addCheckbox("Verbose output", verboseOutput);
        dialog.addHelp(HELP_URL);

        dialog.showDialog();

        if (dialog.wasCanceled()) {
            // No selection
            return;
        }

        verboseOutput = dialog.getNextBoolean();

        final List<String> commandList = new ArrayList<>();
        if (verboseOutput) {
            commandList.add("-v");
        }
        commandList.add(rawFile.getAbsolutePath());

        final ExecProxy.Result r;
        try {
            r = proxy.executeCommand(commandList.toArray(new String[0]));
        } catch (DCRawException e) {
            IJ.error(title, e.getMessage());
            return;
        }

        if (isBlank(r.stdErr)) {
            IJ.log("\n");
            IJ.log(r.stdOut);
            IJ.log("\n");
        } else {
            IJ.error(title, r.stdErr);
        }

    }
}
