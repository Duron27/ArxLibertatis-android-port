If you have any issues find me on discord at @Duron27.

Looking for help with adding features to the launcher. If you can offer any improvements please let me know or push an MR.

This repo and the github one are based on the hard work of many people. most of the credit is not mine.
USE AT YOUR OWN RISK!

Using shaders from:
-   https://github.com/zesterer/openmw-shaders
-   https://gitlab.com/glassmancody.info/omwfx-shaders

Includes:
-   https://gitlab.com/bmwinger/delta-plugin/-/tree/master
-   https://github.com/rfuzzo/plox

USE AT YOUR OWN RISK!

# Building OpenMW for Android in a docker!

-   To build the docker use the command "sudo docker build -t dockerfile . --progress=plain"
-   To run it use "sudo docker run -it dockerfile"
-   The apk will be at the root directory

# Things I plan to add in the future
-   PLOX - load order sorter
-   TES3MP - this is a dream...maybe way way beyond my skills
-   Some kind of cloud syncing for mod list and saves

# New in version 1.7
Added:
-   Lots of fixes including usb storage

Updated openmw to latest

# New in version 1.6
Added:
-   Groundcoverify

Updated openmw to latest June/02/2024

# New in version 1.5
Added:
-   Delta_plugin - fully scripted to run in launcher. just place custom openmw.cfg and in the launcher run delta. mod is created and automatically added to the cfg

# New in version 1.4
Added:
-   Changed from Debug to Release build

Removed:
-   Zacks controller mod until it's finshed

# New in version 1.3
Bug fixes:
-   Fixed android studio not finding libc++
-   Fixed shader setting in wrong location
-   Fixed fog blending - Thanks Sisah

Added:
-   Dxt hardware support - Sisah
-   Better out of water View (Snells window)- Thanks Capo
-   Keystone, going forward you'll be able to update instead of needing to uninstall the old launcher and installing the new one - Thanks Casper!
-   Delta plugin, still working out the in game shell commands sections but the plugin is compiled and works.
-   Latest OpenMW build
-   Manual directory text input if proper path isnt displayed
-   Updated NDK version

# New in version 1.2
Bug fixes:
-   Fixed bug where gl4es would cause a crash - Thanks Sisah

# New in version 1.1
Bug fixes:
-   Fixed bug where Zackhasacat's controller mod would randomly be enabled

Added:
-   Android's built in directory picker

Removed:
-   Storagechooser

# New in version 1.0
Bug fixes:
-   Fixed bug where settings in game are saved after clicking "ok" - Thanks Sisah
-   Fixed bug where screen size wouldn't display properly if cut out area wasn't used - Thanks Sisah
-   Fixed bug where if you reset user config files starting the game would be an endless loop.

Added:
-   Built in shaders in launcher enable fn keys then in game settings enable post processing, then hit F2

Removed:
-   Old code and updated OpenMW    


     