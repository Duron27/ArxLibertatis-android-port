# This repo and the github one are based on the hard work of many people. most of the credit is not mine.

added shaders from 
https://github.com/zesterer/openmw-shaders
https://gitlab.com/glassmancody.info/omwfx-shaders

Lot's of work done by Sisah!
USE AT YOUR OWN RISK!

# OpenMW for Android

To build the docker use the command "sudo docker build -t dockerfile . --progress=plain"
to run it use "sudo docker run -it dockerfile"

the apk will be at the root directrory

# New in version 1.3 - Not released yet
Bug fixes:
    Fixed android studio not finding libc++
    Fixed shader setting in wrong location
    Fixed fog blending - Thanks Sisah

Added:
    Better out of water View (Snells window)- Thanks Capo
    Keystone, now you can update instead of needing to uninstall the old launcher and installing the new one - Thanks Casper!
    Delta plugin, still working out the in game shell commands sections but the plugin is compiled and works.

# New in version 1.2
Bug fixes:
    Fixed bug where gl4es would cause a crash - Thanks Sisah

# New in version 1.1
Bug fixes:
    Fixed bug where Zackhasacat's controller mod would randomly be enabled

Added:
    android's built in directory picker and removed storagechooser

# New in version 1.0
Bug fixes:
    Fixed bug where settings in game are saved after clicking "ok" - Thanks Sisah
    Fixed bug where screen size wouldn't display properly if cut out area wasn't used - Thanks Sisah
    Fixed bug where if you reset user config files starting the game would be an endless loop.

Added:
    built in shaders in launcher enable fn keys then in game settings enable post processing, then hit F2

Removed:
    old code and updated OpenMW        