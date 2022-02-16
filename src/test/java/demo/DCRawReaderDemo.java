/*
 * IJ-Plugins
 * Copyright (C) 2021-2022 Jarek Sacha
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

package demo;

import ij.IJ;
import ij.ImagePlus;
import ij_plugins.dcraw.DCRawException;
import ij_plugins.dcraw.DCRawReader;

import java.io.File;

/**
 * Sample use of DCRaw.
 * <p>
 * Note, you need to provide location of the DCRaw executable, for instance,
 * using system variable <code>dcrawExecutable.path</code>:
 * <pre>
 *   -DdcrawExecutable.path=bin/dcraw_emu.exe
 * </pre>
 */
class DCRawReaderDemo {

    public static void main(String[] args) throws DCRawException {

        // Input file
        final File inFile = new File("../test/data/IMG_5604.CR2");

        // dcraw wrapper
        final DCRawReader dcRawReader = new DCRawReader();

        // Listen to output messages
        dcRawReader.addLogListener(
                m -> System.out.println("message = " + m));

        // Execute dcraw
        dcRawReader.executeCommand(new String[]{
                "-v", // Print verbose messages
                "-w", // Use camera white balance, if possible
                "-T", // Write TIFF instead of PPM
                "-j", // Don't stretch or rotate raw pixels
                "-W", // Don't automatically brighten the image
                inFile.getAbsolutePath()});

        // Cleanup
        dcRawReader.removeAllLogListeners();

        // Load converted file, it is the same location as original raw file but with extension '.tiff'
        final ImagePlus imp = IJ.openImage("test/data/IMG_5604.CR2.tiff");
    }
}

