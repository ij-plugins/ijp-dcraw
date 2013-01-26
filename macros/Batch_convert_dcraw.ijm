/*
 * Image/J Plugins
 * Copyright (C) 2002-2013 Jarek Sacha
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

//
// Use dcraw to batch convert all images in the input directory
//

//Get File Directory and file names
dirSrc = getDirectory("Select Input Directory");
dirDest = getDirectory("Select Output Directory");
fileList = getFileList(dirSrc);
caption = "dcraw batch converter";

print(caption + " - Starting");
print("Reading from : " + dirSrc);
print("Writing to   : " + dirDest);

// Create output directory
File.makeDirectory(dirDest);

setBatchMode(true);
fileNumber = 0;
while (fileNumber < fileList.length) {
    id = fileList[fileNumber++];

    print(toString(fileNumber) + "/" + toString(fileList.length) + ": " + id);

    // Read input image
    run("DCRaw Reader...",
        "open=" + dirSrc + id + " " +
            "use_temporary_directory " +
            "white_balance=[Camera white balance] " +
//            "do_not_automatically_brighten " +
            "output_colorspace=[raw] " +
//            "document_mode " +
//            "document_mode_without_scaling " +
            "read_as=[8-bit] " +
            "interpolation=[High-speed, low-quality bilinear] " +
//            "half_size " +
//            "do_not_rotate " +
//            "show_metadata" +
            "");
    idSrc = getImageID();

    // Save result
    saveAs("Tiff", dirDest + id);

    // Cleanup
    if (isOpen(idSrc)) {
        selectImage(idSrc);
        close();
    }
}
print(caption + " - Completed");

