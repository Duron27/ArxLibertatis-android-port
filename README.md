This repo and the github one are based on the hard work of many people. most of the credit is not mine.
USE AT YOUR OWN RISK!

Using shaders from:
-   https://github.com/zesterer/openmw-shaders
-   https://gitlab.com/glassmancody.info/omwfx-shaders

Includes:
-   https://gitlab.com/bmwinger/delta-plugin/-/tree/master
-   https://github.com/rfuzzo/plox

USE AT YOUR OWN RISK!

# OpenMW for Android
To build the docker use the command "sudo docker build -t dockerfile . --progress=plain"
to run it use "sudo docker run -it dockerfile"

the apk will be at the root directrory

# Things I plan to add in the future
-   PLOX - load order sorter
-   TES3MP - this is a dream...maybe way way beyond my skills
-   Some kind of cloud syncing for mod list and saves
-   Finish up delta shell commands in the launcher

# New in version 1.3 - Not released yet
Bug fixes:
-   Fixed android studio not finding libc++
-   Fixed shader setting in wrong location
-   Fixed fog blending - Thanks Sisah

Added:
-   Better out of water View (Snells window)- Thanks Capo
-   Keystone, now you can update instead of needing to uninstall the old launcher and installing the new one - Thanks Casper!
-   Delta plugin, still working out the in game shell commands sections but the plugin is compiled and works.
-   Latest OpenMW build

# New in version 1.2
Bug fixes:
-   Fixed bug where gl4es would cause a crash - Thanks Sisah

# New in version 1.1
Bug fixes:
-   Fixed bug where Zackhasacat's controller mod would randomly be enabled

Added:
-   android's built in directory picker

Removed:
-   Storagechooser

# New in version 1.0
Bug fixes:
-   Fixed bug where settings in game are saved after clicking "ok" - Thanks Sisah
-   Fixed bug where screen size wouldn't display properly if cut out area wasn't used - Thanks Sisah
-   Fixed bug where if you reset user config files starting the game would be an endless loop.

Added:
-   built in shaders in launcher enable fn keys then in game settings enable post processing, then hit F2

Removed:
-   old code and updated OpenMW    


     