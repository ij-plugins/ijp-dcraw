ijp-dcraw
=========

[![Scala CI](https://github.com/ij-plugins/ijp-dcraw/actions/workflows/scala.yml/badge.svg)](https://github.com/ij-plugins/ijp-dcraw/actions/workflows/scala.yml)

`ijp-dcraw` provides ImageJ plugin "DCRaw Reader" to open raw images from digital cameras. Originally the backend was
provided by [dcraw] tool. Current versions is using [LibRaw]/`dcraw_emu` tool. The hundreds of supported cameras are
listed on [LibRaw Supported Cameras] page.

There are two plugins:

* __DCRaw Reader__ - reads images in camera raw format
* __DCRaw Identify__ - provides info about the raw image, like camera make and metadata contained in the raw file

![Image Calibrator](assets/DCRaw_Reader_Dialog.png)

`ijp-dcraw` distribution, available from [Releases] page, provides native binaries for Windows and macOS. Binaries for other
system can be added manually.

By default, the "DCRaw Reader" plugin looks for the `dcraw_emu` and `raw-identify` executables in the subdirectory `dcraw` of ImageJ plugins folder.
Alternative location can be specified by one of:

1. Setting Java system property `dcrawExecutable.path` and `raw-identifyExecutable.path` to location of the `dcraw` and `raw-identify` executables, for instance:

  ```
    -DdcrawExecutable.path=bin/dcraw_emu.exe -Draw-identifyExecutable.path=bin/raw-identify.exe
  ```

2. or adding property `.dcrawExecutable.path` to ImageJ properties file `IJ_Props.txt`. Note period at the beginning of
   property name, it is required by ImageJ. Example line that should be added to `IJ_Props.txt`

  ```
    .dcrawExecutable.path=C:/apps/bin/dcraw_emu.exe
    .raw-identifyExecutable.path==C:/apps/bin/raw-identify.exe
  ```

Installation
------------

1. Download `ijp-dcraw_plugins_*_win_macos.zip` from the [Releases] page. Binaries, taken from the [LibRaw] release are
   provided for Windows and macOS.
2. Unzip to ImageJ plugins directory. By default, the DCRaw Reader looks for the `dcraw_emu` and `raw-identify` executables in the
   subdirectory "dcraw" of the ImageJ plugins folder.
3. Restart ImageJ

The plugin installs under `Plugins` > `Input-Output` > `DCRaw Reader...`.

Related Plugins
---------------

* [ijp-color] Contains plugin [IJP Color Calibrator] that can be used to color calibrated raw images. For instance, it
  works well with "16-bit linear" images.
* [ijp-DeBayer2SX] an alternative way to demosaic raw images

Tips and Tricks
---------------

### See What Backend Options Passed to `dcraw_emu`

If you wander what is going on behind the scenes, how `dcraw_emu` executable is used, you can see the exact command line
ij-dcraw is executing by turning on the "Debug Mode". Select in ImageJ menu: "Edit" > "Options" > "Misc" and select "
Debug Mode". Now open an image using DCRaw Reader and watch the Log window, it will show the command line with the
options used and the output log generated by `dcraw_emu`.

### Sample ImageJ macro for batch image conversion

Example of using "DCRaw Reader" plugin from an ImageJ macro. The macro converts all images in an input directory,
assumed to be some supported raw format, to TIFF saved in the output directory. The source code for the macro is located
in [macros/Batch_convert_dcraw.ijm].

```javascript
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
        "output_colorspace=[sRGB] " +
        "read_as=[8-bit] " +
        "interpolation=[DHT] " +
        //            "half_size " +
        //            "do_not_rotate " +
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
```

### Using ij-dcraw directly from Java

Example of using `DCRawReader` API from a Java code. The source code is located
in [src/test/java/demo/DCRawReaderDemo.java].

```java
import ij.ImagePlus;
import ij_plugins.dcraw.DCRawException;
import ij_plugins.dcraw.DCRawReader;

import java.io.File;

class DCRawReaderDemo {

    public static void main(String[] args) throws DCRawException {

        final File inFile = new File("../test/data/IMG_5604.CR2");
        final ImagePlus imp = new DCRawReader().read(inFile);
    }
}
```

### Using low-level API to call DCRaw

You can also call `dcraw` by passing command line options. This may give access to some functionality
that may not be yet exposed through higher level `DCRawReader` API.

```java
import ij.IJ;
import ij.ImagePlus;
import ij_plugins.dcraw.DCRawException;
import ij_plugins.dcraw.util.ExecProxy;

import java.io.File;
import java.util.Optional;

import static ij_plugins.dcraw.IJPUtils.isBlank;

/**
 * Example of calling dcraw executable directly, passing command native line options
 */
public class DCRawExecProxyDemo {
    public static void main(String[] args) throws DCRawException {
        // Input file
        final File inFile = new File("../test/data/IMG_5604.CR2");

        // Output file that will be generated by dcraw
        final File outputFile = new File(inFile.getAbsolutePath() + ".tiff");

        // dcraw wrapper
        final ExecProxy proxy = new ExecProxy("dcraw_emu", "dcrawExecutable.path",
                Optional.of(m -> System.out.println("status: " + m)),
                Optional.of(m -> System.out.println("error : " + m)));

        // Execute dcraw
        ExecProxy.Result r = proxy.executeCommand(new String[]{
                "-v",      // Print verbose messages
                "-q", "0", // Use high-speed, low-quality bilinear interpolation.
                "-w",      // Use camera white balance, if possible
                "-T",      // Write TIFF instead of PPM
                "-j",      // Don't stretch or rotate raw pixels
                "-W",      // Don't automatically brighten the image
                inFile.getAbsolutePath()});

        System.out.println("dcraw_emu stdErr: '" + r.stdErr + "'");
        System.out.println("dcraw_emu stdOut:\n" + r.stdOut);

        if (isBlank(r.stdErr)) {
            // Load converted file, it is the same location as original raw file but with extension '.tiff'
            final ImagePlus imp = IJ.openImage(outputFile.getAbsolutePath());
            System.out.println("Loaded converted raw file: " + imp.getWidth() + " by " + imp.getHeight());
        }
    }
}
```

Notes
-----

ijp-dcraw project was originally hosted on [SourceForge]. Releases 1.5 and older can be found [there][SourceForge].

[dcraw]: https://en.wikipedia.org/wiki/Dcraw

[LibRaw]: https://www.libraw.org/about

[LibRaw Supported Cameras]: https://www.libraw.org/supported-cameras

[Releases]: https://github.com/ij-plugins/ijp-dcraw/releases

[SourceForge]: http://ij-plugins.sourceforge.net/plugins/dcraw/index.html

[ijp-color]: https://github.com/ij-plugins/ijp-color

[ijp-DeBayer2SX]: https://github.com/ij-plugins/ijp-DeBayer2SX

[IJP Color Calibrator]: https://github.com/ij-plugins/ijp-color/wiki/Color-Calibrator

[macros/Batch_convert_dcraw.ijm]: https://github.com/ij-plugins/ijp-dcraw/blob/master/macros/Batch_convert_dcraw.ijm

[src/test/java/demo/DCRawReaderDemo.java]: https://github.com/ij-plugins/ijp-dcraw/blob/master/src/test/java/demo/DCRawReaderDemo.java