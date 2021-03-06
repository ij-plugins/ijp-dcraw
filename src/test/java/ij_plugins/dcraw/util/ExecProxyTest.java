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

package ij_plugins.dcraw.util;

import ij.IJ;
import ij.ImagePlus;
import ij_plugins.dcraw.DCRawException;
import org.junit.Test;

import java.io.File;
import java.util.Optional;

import static org.junit.Assert.*;


/**
 * For this test to work you may have to set system property 'dcrawExecutable.path' to absolute location of
 * dcraw_emu executable.
 * It can be done, for instance, through a command line options '-DdcrawExecutable.path=/bin/dcraw_emu'
 *
 * @author Jarek Sacha
 */
public final class ExecProxyTest {

    @Test
    public void testValidateDCRaw() {
        final ExecProxy proxy = new ExecProxy("dcraw_emu", "dcrawExecutable.path",
                Optional.of(m -> System.out.println("status: " + m)),
                Optional.of(m -> System.out.println("error : " + m)));
        try {
            proxy.validateExecutable();
            assertTrue(true);
        } catch (DCRawException ex) {
            fail(ex.getMessage());
        }
    }


    @Test
    public void testExecuteCommand() throws DCRawException {

        // Input file
        final File inFile = new File("../test/data/IMG_5604.CR2");
        assertTrue("File exists: " + inFile.getAbsolutePath(), inFile.exists());

        // Output file that will be generated by dcraw
        final File outputFile = new File(inFile.getAbsolutePath() + ".tiff");
        if (outputFile.exists())
            if (!outputFile.delete())
                fail("Unable to delete output file: " + outputFile.getAbsolutePath());
        assertFalse("Output file should not exists: " + outputFile.getAbsolutePath(), outputFile.exists());

        // dcraw wrapper
        final ExecProxy proxy = new ExecProxy("dcraw_emu", "dcrawExecutable.path",
                Optional.of(m -> System.out.println("status: " + m)),
                Optional.of(m -> System.out.println("error : " + m)));

        // Execute dcraw
        proxy.executeCommand(new String[]{
                "-v",      // Print verbose messages
                "-q", "0", // Use high-speed, low-quality bilinear interpolation.
                "-w",      // Use camera white balance, if possible
                "-T",      // Write TIFF instead of PPM
                "-j",      // Don't stretch or rotate raw pixels
                "-W",      // Don't automatically brighten the image
                inFile.getAbsolutePath()});

        // Load converted file, it is the same location as original raw file but with extension '.tiff'
        assertTrue("Output file should exists: " + outputFile.getAbsolutePath(), outputFile.exists());
        final ImagePlus imp = IJ.openImage(outputFile.getAbsolutePath());
        assertNotNull("Cannot load TIFF image file: " + outputFile.getAbsolutePath(), imp);
    }
}