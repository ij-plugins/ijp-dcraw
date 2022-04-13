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

import ij.ImagePlus;
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
 * @since 8/5/11 3:23 PM
 */
public final class DCRawReaderTest {

    @Test
    public void testValidateDCRaw() {
        final DCRawReader dcRaw = new DCRawReader();
        try {
            dcRaw.validateDCRawExec();
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

        final File outputFile = new File(inFile.getAbsolutePath() + ".tiff");
        if (outputFile.exists())
            if (!outputFile.delete())
                fail("Unable to delete output file: " + outputFile.getAbsolutePath());
        assertFalse("Output file should not exists: " + outputFile.getAbsolutePath(), outputFile.exists());

        LogListener logListener = message -> System.out.println("MESSAGE : " + message);
        LogListener errorListener = message -> System.out.println("ERROR : " + message);

        // dcraw wrapper
        final DCRawReader dcRaw = new DCRawReader(Optional.of(logListener), Optional.of(errorListener));

        final DCRawReader.Config config = new DCRawReader.Config();
        config.interpolationQuality = DCRawReader.InterpolationQualityOption.HIGH_SPEED;
        config.halfSize = true;
        config.outputColorSpace = DCRawReader.OutputColorSpaceOption.SRGB;

        final ImagePlus imp = dcRaw.read(inFile, config);
        assertNotNull(imp);
    }
}