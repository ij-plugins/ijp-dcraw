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
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

import java.io.File;
import java.util.Optional;

import static ij_plugins.dcraw.DCRawReader.*;


/**
 * Plugin for opening RAW images. It calls DCRAW to convert a RAW image to PPM then loads that PPM image.
 * <br>
 * The home site for DCRAW is now <a href="https://www.libraw.org/"></a>.
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
            "For more information about \"DCRaw Reader\" see project page at <a href=\"" + HELP_URL + "\">" + HELP_URL + "</a> " +
            "</p>" +
            "</html>";
    private static DCRawReader.Config CONFIG = new DCRawReader.Config();

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

        LogListener statusLogger = message -> {
            IJ.showStatus("DCRaw: " + message);
            if (IJ.debugMode) IJ.log("DCRaw: " + message);
        };
        LogListener errorLogger = message -> {
            IJ.showStatus("ERROR DCRaw: " + message);
            if (IJ.debugMode) IJ.log("ERROR DCRaw: " + message);
        };

        final DCRawReader dcRawReader = new DCRawReader(Optional.of(statusLogger), Optional.of(errorLogger));

        // Verify that could talk to DCRAW before asking the user to select options
        try {
            dcRawReader.validateDCRawExec();
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
        IJ.showStatus("Converting RAW file: " + rawFile.getName());

        final Optional<DCRawReader.Config> dstConfig = showDialog(CONFIG, title);

        dstConfig.ifPresent(config -> {
            CONFIG = config;
            try {
                final ImagePlus dst = dcRawReader.read(rawFile, config);
                statusLogger.log("Opening RAW file: " + rawFile.getName());
                dst.show();
            } catch (final DCRawException e) {
                e.printStackTrace();
                errorLogger.log(e.getMessage());
                IJ.error(TITLE, e.getMessage());
                IJ.showMessage("About " + title, ABOUT);

            }
        });
    }

    private Optional<DCRawReader.Config> showDialog(final DCRawReader.Config oldConfig, final String title) {
        //
        // Setup options dialog
        //
        final GenericDialog dialog = new GenericDialog(title);
        dialog.setIconImage(IJPUtils.imageJIconAsAWTImage());

        dialog.addPanel(IJPUtils.createInfoPanel(TITLE, HTML_DESCRIPTION));

        dialog.addCheckbox("Use_temporary_directory for processing", CONFIG.useTmpDir);

        // Auto white balance
        dialog.addChoice("White_balance", asStrings(WhiteBalanceOption.values()),
                oldConfig.whiteBalance.toString());

        dialog.addCheckbox("Do_not_automatically_brighten the image",
                oldConfig.doNotAutomaticallyBrightenTheImage);

        // -o [0-5]  Output colorspace (raw,sRGB,Adobe,Wide,ProPhoto,XYZ)
        dialog.addChoice("Output_colorspace", asStrings(OutputColorSpaceOption.values()),
                oldConfig.outputColorSpace.toString());

        // Image bit format
        dialog.addChoice("Read_as", asStrings(FormatOption.values()), oldConfig.format.toString());

        // Interpolation quality
        dialog.addChoice("Interpolation quality", asStrings(InterpolationQualityOption.values()),
                oldConfig.interpolationQuality.toString());

        dialog.addCheckbox("Half_size", oldConfig.halfSize);

        dialog.addCheckbox("Do_not_rotate or scale pixels (preserve orientation and aspect ratio)",
                oldConfig.doNotStretchOrRotate);

        dialog.addChoice("Flip image", asStrings(FlipImage.values()),
                oldConfig.flipImage.toString());

        dialog.addHelp(HELP_URL);

        //
        // Show dialog
        //
        dialog.showDialog();

        if (dialog.wasCanceled()) {
            // No selection
            return Optional.empty();
        }

        final DCRawReader.Config dstConfig = new DCRawReader.Config();
        dstConfig.useTmpDir = dialog.getNextBoolean();
        dstConfig.whiteBalance = WhiteBalanceOption.byName(dialog.getNextChoice());
        dstConfig.doNotAutomaticallyBrightenTheImage = dialog.getNextBoolean();
        dstConfig.outputColorSpace = OutputColorSpaceOption.byName(dialog.getNextChoice());
        dstConfig.format = FormatOption.byName(dialog.getNextChoice());
        dstConfig.interpolationQuality = InterpolationQualityOption.byName(dialog.getNextChoice());
        dstConfig.halfSize = dialog.getNextBoolean();
        dstConfig.doNotStretchOrRotate = dialog.getNextBoolean();
        dstConfig.flipImage = FlipImage.byName(dialog.getNextChoice());

        return Optional.of(dstConfig);
    }
}
