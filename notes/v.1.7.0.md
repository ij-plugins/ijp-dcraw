v.1.7.0
========

New features:
* Read raw metadata using 'raw-identify' tool [#7]
* Friendlier API for using DCRawReader from Java [#11]
* Improve Java 8 binary compatibility [#12]
* DCRaw Reader supports image flipping options [#14]

Bug fixes:
* Some runtime errors are not show to the user [#13]

Plugin installation:
1. Download `ijp-dcraw_plugins_*_win_macos.zip` from the [Releases] page. Binaries, taken from the [LibRaw] release are provided for Windows and macOS.
2. Unzip to ImageJ plugins directory. By default, the DCRaw Reader looks for the `dcraw_emu` binary in the subdirectory "dcraw" of the ImageJ plugins folder.
3. Restart ImageJ

[LibRaw]: https://www.libraw.org/about

[#7]: https://github.com/ij-plugins/ijp-dcraw/issues/7
[#11]: https://github.com/ij-plugins/ijp-dcraw/issues/11
[#12]: https://github.com/ij-plugins/ijp-dcraw/issues/12
[#13]: https://github.com/ij-plugins/ijp-dcraw/issues/13
[#14]: https://github.com/ij-plugins/ijp-dcraw/issues/14