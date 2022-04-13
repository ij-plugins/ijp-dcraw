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

import junit.framework.TestCase;

/**
 * @author Jarek Sacha
 */
public class DCRawVersionTest extends TestCase {
    public DCRawVersionTest(String test) {
        super(test);
    }

    public void testInstance() {
        assertNotNull(DCRawVersion.getInstance());
    }

    public void testToString() {
        final DCRawVersion instance = DCRawVersion.getInstance();
        assertNotNull(instance);

        final String version = DCRawVersion.getInstance().toString();
        assertNotNull(version);

        final int firstDotIndex = version.indexOf('.');
        assertTrue(firstDotIndex > 0);

        final int majorVersionNumber = Integer.parseInt(version.substring(0, firstDotIndex));
        assertTrue(majorVersionNumber >= 0);

        final int secondDotIndex = version.indexOf('.', firstDotIndex + 1);
        assertTrue(secondDotIndex > 0);

        final int minorVersionNumber = Integer.parseInt(version.substring(firstDotIndex + 1, secondDotIndex));
        assertTrue(minorVersionNumber >= 0);

        final int firstSpace = version.indexOf(' ', secondDotIndex + 1);
        final int updateVersionNumber;
        if (firstSpace < 0) {
            final int thirdDotIndex = version.indexOf('.', secondDotIndex + 1);
            if (thirdDotIndex > 0) {
                // Final release version number for instance "1.5.2.0-SNAPSHOT"
                updateVersionNumber = Integer.parseInt(version.substring(secondDotIndex + 1, thirdDotIndex));
            } else {
                // Final release version number for instance "1.5.2"
                updateVersionNumber = Integer.parseInt(version.substring(secondDotIndex + 1));
            }
        } else {
            // Interim release version number for instance "1.5.2 dev"
            updateVersionNumber = Integer.parseInt(version.substring(secondDotIndex + 1, firstSpace));
        }

        assertTrue(updateVersionNumber >= 0);
    }

}