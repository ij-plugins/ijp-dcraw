IJ Plugins Toolkit
==================

[ij-dcraw](http://ij-plugins.sourceforge.net/plugins/dcraw/index.html) plugin open raw images from digital cameras in
ImageJ with a help of [dcraw](http://www.cybercom.net/~dcoffin/dcraw/) tool.
The dcraw is a C program created by David Coffin. Over 500 various raw formats are supported, including Adobe DNG.
Full list can be found at dcraw home page in section [Supported Cameras](http://www.cybercom.net/~dcoffin/dcraw/#cameras).

You can control how raw or how processed the opened image is.
This enables extracting sensor data that is lost when image is converted to a standard viewing format.
You can chose to concentrate on image analysis rather than on making an eye-pleasing pictures.

The plugin installs under `Plugins` > `Input-Output` > `DCRaw Reader`.

For more information see [ij-dcraw home page](http://ij-plugins.sourceforge.net/plugins/dcraw/index.html).


Release files
-------------

* `ij-dcraw_bin_1.4.0.r*.zip` - only ij-dcraw plugin, without dcraw executable.
* `ij-dcraw_native-bin-windows_1.4.0.r*.zip` - ij-dcraw plugin with Windows dcraw executable.
* `ij-dcraw_native-bin-linux_1.4.0.r*.tar.gz` - ij-dcraw plugin with Linux binaries (Ubuntu x64).
* `ij-dcraw_src_1.4.0.r*.zip` - sources.


Installation
------------

1. Download `ij-dcraw_bin_*`
2. Unzip to ImageJ plugins directory.
By default, the Reader looks for the dcraw binary in subdirectory dcraw of ImageJ plugins folder.
An alternative location can be specified by adding property `.dcrawExecutable.path` to
ImageJ properties file `IJ_Props.txt` located in ImageJ home directory. Here is an example:
`.dcrawExecutable.path=/apps/dcraw/dcraw`
Note period at the beginning of property name, it is required by ImageJ.
3. Restart ImageJ
