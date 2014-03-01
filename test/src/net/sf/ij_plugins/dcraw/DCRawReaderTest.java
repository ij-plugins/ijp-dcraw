/*
 * Image/J Plugins
 * Copyright (C) 2002-2014 Jarek Sacha
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
import ij.ImagePlus;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;


/**
 * For this test to work you may have to set system property 'dcrawExecutable.path' to absolute location of
 * dcraw executable.  It can be done, for instance, through a command line options '-DdcrawExecutable.path=/bin/dcraw'
 *
 * @author Jarek Sacha
 * @since 8/5/11 3:23 PM
 */
public final class DCRawReaderTest {

    @Test
    public void testValidateDCRaw() throws Exception {
        final DCRawReader dcRawReader = new DCRawReader();
        try {
            dcRawReader.validateDCRaw();
            assertTrue(true);
        } catch (DCRawException ex) {
            fail(ex.getMessage());
        }
    }


    @Test
    public void testExecuteCommand() throws DCRawException {

        // Input file
        final File inFile = new File("test/data/IMG 56 04.CR2");
        assertTrue("File exists: " + inFile.getAbsolutePath(), inFile.exists());

        final File outputFile = new File("test/data/IMG 56 04.tiff");
        if (outputFile.exists())
            if (!outputFile.delete())
                fail("Unable to delete output file: " + outputFile.getAbsolutePath());
        assertFalse("Output file should not exists: " + outputFile.getAbsolutePath(), outputFile.exists());

        // dcraw wrapper
        final DCRawReader dcRawReader = new DCRawReader();

        // Listen to output messages
        dcRawReader.addLogListener(new DCRawReader.LogListener() {
            @Override
            public void log(String message) {
                System.out.println("message = " + message);
            }
        });

        // Execute dcraw
        dcRawReader.executeCommand(new String[]{
                "-v", // Print verbose messages
                "-w", // Use camera white balance, if possible
                "-T", // Write TIFF instead of PPM
                "-j", // Don't stretch or rotate raw pixels
                "-W", // Don't automatically brighten the image
                '"' + inFile.getAbsolutePath() + '"'});

        // Cleanup
        dcRawReader.removeAllLogListeners();

        // Load converted file, it is the same location as original raw file but with extension '.tiff'
        assertTrue("Output file should exists: " + outputFile.getAbsolutePath(), outputFile.exists());
        final ImagePlus imp = IJ.openImage(outputFile.getAbsolutePath());
        assertNotNull("Cannot load TIFF image file: " + outputFile.getAbsolutePath(), imp != null);
    }
}