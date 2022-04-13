/*
 * IJ-Plugins
 * Copyright (C) 2022-2022 Jarek Sacha
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

import ij_plugins.dcraw.DCRawException;
import ij_plugins.dcraw.util.ExecProxy;

import java.io.File;
import java.util.Optional;

/**
 * Example of calling dcraw executable directly, passing command native line options
 */
public class RawIdentifyExecProxyDemo {
    public static void main(String[] args) throws DCRawException {
        // Input file
        final File inFile = new File("../test/data/IMG_5604.CR2");

        // raw-identify wrapper
        final ExecProxy proxy = new ExecProxy("raw-identify",
                "raw-identifyExecutable.path",
                Optional.of(m -> System.out.println("status: " + m)),
                Optional.of(m -> System.out.println("error : " + m)));

        // Execute dcraw
        ExecProxy.Result r = proxy.executeCommand(new String[]{
//                "-c", // compact output
//                "-n", // print make/model and norm. make/model
                "-v", // verbose output
//                "-w", // print white balance
//                "-j", // print JSON
//                "-u", // print unpack function
//                "-f", // print frame size (only w/ -u)
//                "-s", // print output image size
//                "-h", // force half-size mode (only for -s)
//                "-M", // disable use of raw-embedded color data
//                "+M", // force use of raw-embedded color data
                inFile.getAbsolutePath()});

        System.out.println("raw-identify stdErr:\n" + r.stdErr);
        System.out.println("raw-identify stdOut:\n" + r.stdOut);
    }
}
