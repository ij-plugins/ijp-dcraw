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

package demo;

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

//        final File inFile = new File("../test/data/IMG_5604.CR2");
        final File inFile = new File("C:/Users/ac4566/Osobiste/Projects/IJ-Plugins/ij-plugins.github/ijp-dcraw/binaries/windows/raw-identify.exe");
        final ImagePlus imp = new DCRawReader().read(inFile);
        System.out.println("Loaded converted raw file: " + imp.getWidth() + " by " + imp.getHeight());
    }
}

