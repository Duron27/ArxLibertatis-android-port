# syntax=docker/dockerfile:labs
FROM rockylinux:9

#Set build type : release, debug
ENV BUILD_TYPE=release

# App versions - change settings here
ENV LIBJPEG_TURBO_VERSION=3.0.2
ENV LIBPNG_VERSION=1.6.42
ENV FREETYPE2_VERSION=2.13.2
ENV OPENAL_VERSION=1.23.1
ENV BOOST_VERSION=1.85.0
ENV LIBICU_VERSION=70-1
ENV FFMPEG_VERSION=6.1
ENV SDL2_VERSION=2.24.0
ENV BULLET_VERSION=3.25
ENV ZLIB_VERSION=1.3.1
ENV LIBXML2_VERSION=2.12.5
ENV MYGUI_VERSION=3.4.3
ENV GL4ES_VERSION=1.1.8
ENV COLLADA_DOM_VERSION=2.5.0
ENV OSG_VERSION=3.6.5.1
ENV LZ4_VERSION=1.9.3
ENV LUAJIT_VERSION=2.1.ROLLING
ENV OPENMW_VERSION=061f10bef7200965e6cfed4882dafa83bd1f6366
ENV NDK_VERSION=26.3.11579264
ENV SDK_CMDLINE_TOOLS=10406996_latest
ENV PLATFORM_TOOLS_VERSION=29.0.0
ENV JAVA_VERSION=17

# Version of Release
ARG APP_VERSION=unknown

RUN dnf install -y dnf-plugins-core && dnf config-manager --set-enabled crb && dnf install -y epel-release
RUN dnf install -y https://mirrors.rpmfusion.org/free/el/rpmfusion-free-release-9.noarch.rpm \
    && dnf install -y xz p7zip bzip2 libstdc++-devel glibc-devel zip unzip libcurl-devel java-11-openjdk which wget python-devel doxygen nano gcc-c++ git java-${JAVA_VERSION}-openjdk cmake patch

RUN alternatives --set java java-17-openjdk.x86_64
RUN JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which java)))))
ENV ANDROID_SDK_ROOT=/root/Android/cmdline-tools/latest/bin
ENV ANDROID_HOME=/root/Android
RUN mkdir -p ${HOME}/prefix
RUN mkdir -p ${HOME}/src

# Set the installation Dir
ENV PREFIX=/root/prefix
RUN cd ${HOME}/src && wget https://github.com/unicode-org/icu/archive/refs/tags/release-${LIBICU_VERSION}.zip && unzip -o ${HOME}/src/release-${LIBICU_VERSION}.zip && rm -rf release-${LIBICU_VERSION}.zip
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-${SDK_CMDLINE_TOOLS}.zip && unzip commandlinetools-linux-${SDK_CMDLINE_TOOLS}.zip && mkdir -p ${HOME}/Android/cmdline-tools/ && mv cmdline-tools/ ${HOME}/Android/cmdline-tools/latest && rm commandlinetools-linux-${SDK_CMDLINE_TOOLS}.zip
RUN yes | ~/Android/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null
RUN ~/Android/cmdline-tools/latest/bin/sdkmanager --install "ndk;${NDK_VERSION}" "platforms;android-28" "platform-tools" "build-tools;29.0.2" --channel=0
RUN yes | ~/Android/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null

#Setup ICU for the Host
RUN mkdir -p ${HOME}/src/icu-host-build && cd $_ && ${HOME}/src/icu-release-70-1/icu4c/source/configure --disable-tests --disable-samples --disable-icuio --disable-extras CC="gcc" CXX="g++" && make -j $(nproc)
ENV PATH=$PATH:/root/Android/cmdline-tools/latest/bin/:/root/Android/ndk/${NDK_VERSION}/:/root/Android/ndk/${NDK_VERSION}/toolchains/llvm/prebuilt/linux-x86_64:/root/Android/ndk/${NDK_VERSION}/toolchains/llvm/prebuilt/linux-x86_64/bin:/root/prefix/include:/root/prefix/lib:/root/prefix/:/root/.cargo/bin

# NDK Settings
ENV API=24
ENV ABI=arm64-v8a
ENV ARCH=aarch64
ENV NDK_TRIPLET=${ARCH}-linux-android
ENV TOOLCHAIN=/root/Android/ndk/${NDK_VERSION}/toolchains/llvm/prebuilt/linux-x86_64
ENV NDK_SYSROOT=${TOOLCHAIN}/sysroot/
ENV ANDROID_SYSROOT=${TOOLCHAIN}/sysroot/
 # ANDROID_NDK is needed for SDL2 cmake
ENV ANDROID_NDK=/root/Android/ndk/${NDK_VERSION}/
ENV AR=${TOOLCHAIN}/bin/llvm-ar
ENV LD=${TOOLCHAIN}/bin/ld
ENV RANLIB=${TOOLCHAIN}/bin/llvm-ranlib
ENV STRIP=${TOOLCHAIN}/bin/llvm-strip
ENV CC=${TOOLCHAIN}/bin/${NDK_TRIPLET}${API}-clang
ENV CXX=${TOOLCHAIN}/bin/${NDK_TRIPLET}${API}-clang++
ENV clang=${TOOLCHAIN}/bin/${NDK_TRIPLET}${API}-clang
ENV clang++=${TOOLCHAIN}/bin/${NDK_TRIPLET}${API}-clang++
ENV PKG_CONFIG_LIBDIR=${PREFIX}/lib/pkgconfig

# Global C, CXX and LDFLAGS
ENV CFLAGS="-fPIC -O3 -flto=thin"
ENV CXXFLAGS="-fPIC -O3 -frtti -fexceptions -flto=thin"
ENV LDFLAGS="-fPIC -Wl,--undefined-version -flto=thin -fuse-ld=lld"

ENV COMMON_CMAKE_ARGS \
  "-DCMAKE_TOOLCHAIN_FILE=/root/Android/ndk/${NDK_VERSION}/build/cmake/android.toolchain.cmake" \
  "-DANDROID_ABI=${ABI}" \
  "-DANDROID_PLATFORM=${API}" \
  "-DANDROID_STL=c++_shared" \
  "-DANDROID_CPP_FEATURES=" \
  "-DANDROID_ALLOW_UNDEFINED_VERSION_SCRIPT_SYMBOLS=ON" \
  "-DCMAKE_BUILD_TYPE=$BUILD_TYPE" \
  "-DCMAKE_C_FLAGS=-I${PREFIX}" \
  "-DCMAKE_DEBUG_POSTFIX=" \
  "-DCMAKE_INSTALL_PREFIX=${PREFIX}" \
  "-DCMAKE_FIND_ROOT_PATH=${PREFIX}" \
  "-DCMAKE_CXX_COMPILER=${NDK_TRIPLET}${API}-clang++" \
  "-DCMAKE_CC_COMPILER=${NDK_TRIPLET}${API}-clang" \
  "-DHAVE_LD_VERSION_SCRIPT=OFF"

ENV COMMON_AUTOCONF_FLAGS="--enable-static --disable-shared --prefix=${PREFIX} --host=${NDK_TRIPLET}${API}"

ENV NDK_BUILD_FLAGS \
    "NDK_PROJECT_PATH=." \
    "APP_BUILD_SCRIPT=./Android.mk" \
    "APP_PLATFORM=${API}" \
    "APP_ABI=${ABI}"

# Setup rust build system for android
RUN wget https://sh.rustup.rs -O rustup.sh && sha256sum rustup.sh && \
    echo "32a680a84cf76014915b3f8aa44e3e40731f3af92cd45eb0fcc6264fd257c428  rustup.sh" | sha256sum -c - && \
    sh rustup.sh -y && rm rustup.sh && \
    ${HOME}/.cargo/bin/rustup target add ${NDK_TRIPLET} && \
    ${HOME}/.cargo/bin/rustup toolchain install nightly && \
    ${HOME}/.cargo/bin/rustup target add --toolchain nightly ${NDK_TRIPLET} && \
    echo "[target.${NDK_TRIPLET}]" >> /root/.cargo/config && \
    echo "linker = \"${TOOLCHAIN}/bin/${NDK_TRIPLET}${API}-clang\"" >> /root/.cargo/config

# Setup LIBICU
RUN mkdir -p ${HOME}/src/icu-${LIBICU_VERSION} && cd $_ && \
    ${HOME}/src/icu-release-${LIBICU_VERSION}/icu4c/source/configure \
        ${COMMON_AUTOCONF_FLAGS} \
        --disable-tests \
        --disable-samples \
        --disable-icuio \
        --disable-extras \
        --prefix=${PREFIX} \
        --with-cross-build=/root/src/icu-host-build && \
    make -j $(nproc) check_PROGRAMS= bin_PROGRAMS= && \
    make install check_PROGRAMS= bin_PROGRAMS=

# Setup Bzip2
RUN cd $HOME/src/ && git clone https://github.com/libarchive/bzip2 && cd bzip2 && cmake . $COMMON_CMAKE_ARGS && make -j $(nproc) && make install

# Setup ZLIB
RUN wget -c https://github.com/madler/zlib/archive/refs/tags/v${ZLIB_VERSION}.tar.gz -O - | tar -xz -C $HOME/src/ && \
    mkdir -p ${HOME}/src/zlib-${ZLIB_VERSION}/build && cd $_ && \
    cmake ${HOME}/src/zlib-${ZLIB_VERSION} \
        ${COMMON_CMAKE_ARGS} && \
    make -j $(nproc) && make install

# Setup LIBJPEG_TURBO
RUN wget -c https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/${LIBJPEG_TURBO_VERSION}/libjpeg-turbo-${LIBJPEG_TURBO_VERSION}.tar.gz -O - | tar -xz -C $HOME/src/ && \
    mkdir -p ${HOME}/src/libjpeg-turbo-${LIBJPEG_TURBO_VERSION}/build && cd $_ && \
    cmake ${HOME}/src/libjpeg-turbo-${LIBJPEG_TURBO_VERSION} \
        ${COMMON_CMAKE_ARGS} \
        -DENABLE_SHARED=false && \
    make -j $(nproc) && make install

# Setup LIBPNG
RUN wget -c http://prdownloads.sourceforge.net/libpng/libpng-${LIBPNG_VERSION}.tar.gz -O - | tar -xz -C $HOME/src/ && \
    mkdir -p ${HOME}/src/libpng-${LIBPNG_VERSION}/build && cd $_ && \
        ${HOME}/src/libpng-${LIBPNG_VERSION}/configure \
        ${COMMON_AUTOCONF_FLAGS} && \
    make -j $(nproc) check_PROGRAMS= bin_PROGRAMS= && \
    make install check_PROGRAMS= bin_PROGRAMS=

# Setup FREETYPE2
RUN wget -c http://prdownloads.sourceforge.net/freetype/freetype-${FREETYPE2_VERSION}.tar.gz -O - | tar -xz -C $HOME/src/ && \
    mkdir -p ${HOME}/src/freetype-${FREETYPE2_VERSION}/build && cd $_ && \
        ${HOME}/src/freetype-${FREETYPE2_VERSION}/configure \
        ${COMMON_AUTOCONF_FLAGS} \
        --with-png=no && \
    make -j $(nproc) && make install

# Setup LIBXML
RUN wget -c https://github.com/GNOME/libxml2/archive/refs/tags/v${LIBXML2_VERSION}.tar.gz -O - | tar -xz -C $HOME/src/ && \
    mkdir -p ${HOME}/src/libxml2-${LIBXML2_VERSION}/build && cd $_ && \
    cmake ${HOME}/src/libxml2-${LIBXML2_VERSION} \
        ${COMMON_CMAKE_ARGS} \
        -DBUILD_SHARED_LIBS=OFF \
        -DLIBXML2_WITH_THREADS=ON \
        -DLIBXML2_WITH_CATALOG=OFF \
        -DLIBXML2_WITH_ICONV=OFF \
        -DLIBXML2_WITH_LZMA=OFF \
        -DLIBXML2_WITH_PROGRAMS=OFF \
        -DLIBXML2_WITH_PYTHON=OFF \
        -DLIBXML2_WITH_TESTS=OFF \
        -DLIBXML2_WITH_ZLIB=ON && \
    make -j $(nproc) && make install

# Setup OPENAL
RUN wget -c https://github.com/kcat/openal-soft/archive/${OPENAL_VERSION}.tar.gz -O - | tar -xz -C $HOME/src/ && \
    mkdir -p ${HOME}/src/openal-soft-${OPENAL_VERSION}/build && cd $_ && \
    cmake ${HOME}/src/openal-soft-${OPENAL_VERSION} \
        ${COMMON_CMAKE_ARGS} \
        -DALSOFT_EXAMPLES=OFF \
        -DALSOFT_TESTS=OFF \
        -DALSOFT_UTILS=OFF \
        -DALSOFT_NO_CONFIG_UTIL=ON \
        -DALSOFT_BACKEND_OPENSL=ON \
        -DALSOFT_BACKEND_WAVE=OFF && \
    make -j $(nproc) && make install

# Setup BOOST
RUN wget -c https://github.com/boostorg/boost/releases/download/boost-${BOOST_VERSION}/boost-${BOOST_VERSION}-cmake.tar.gz -O - | tar -xz -C $HOME/src/ && \
    mkdir -p ${HOME}/src/boost-${BOOST_VERSION}/build && cd $_ && \
    cmake ../ ${COMMON_CMAKE_ARGS} \
        -DBOOST_INCLUDE_LIBRARIES="filesystem;program_options;iostreams;geometry" && \
    make -j $(nproc) && make install
# build system and regex
RUN llvm-ar rc ${PREFIX}/lib/libboost_system.a $(find / -name "error_code.o" 2>/dev/null)
RUN llvm-ar rc ${PREFIX}/lib/libboost_regex.a $(find / \( -name "posix_api.o" -o -name "regex.o" -o -name "regex_debug.o" -o -name "static_mutex.o" -o -name "wide_posix_api.o" \) 2>/dev/null)
RUN $RANLIB ${PREFIX}/lib/libboost_{system,filesystem,program_options,iostreams,regex}.a

# Setup FFMPEG_VERSION
RUN wget -c http://ffmpeg.org/releases/ffmpeg-${FFMPEG_VERSION}.tar.bz2 -O - | tar -xjf - -C ${HOME}/src/ && \
    mkdir -p ${HOME}/src/ffmpeg-${FFMPEG_VERSION} && cd $_ && \
    ${HOME}/src/ffmpeg-${FFMPEG_VERSION}/configure \
        --disable-asm \
        --disable-optimizations \
        --target-os=android \
        --enable-cross-compile \
        --cross-prefix=${TOOLCHAIN}/bin/llvm- \
        --cc=${NDK_TRIPLET}${API}-clang \
        --arch=arm64 \
        --cpu=armv8-a \
        --prefix=${PREFIX} \
        --enable-version3 \
        --enable-pic \
        --disable-everything \
        --disable-doc \
        --disable-programs \
        --disable-autodetect \
        --disable-iconv \
        --enable-decoder=mp3 \
        --enable-demuxer=mp3 \
        --enable-decoder=bink \
        --enable-decoder=binkaudio_rdft \
        --enable-decoder=binkaudio_dct \
        --enable-demuxer=bink \
        --enable-demuxer=wav \
        --enable-decoder=pcm_* \
        --enable-decoder=vp8 \
        --enable-decoder=vp9 \
        --enable-decoder=opus \
        --enable-decoder=vorbis \
        --enable-demuxer=matroska \
        --enable-demuxer=ogg && \
    make -j $(nproc) && make install

# Setup SDL2_VERSION
RUN wget -c https://github.com/libsdl-org/SDL/releases/download/release-${SDL2_VERSION}/SDL2-${SDL2_VERSION}.tar.gz -O - | tar -xz -C ${HOME}/src/ && \
    mkdir -p ${HOME}/src/SDL2-${SDL2_VERSION}/build && cd $_ && \
    cmake ../ ${COMMON_CMAKE_ARGS} \
        -DSDL_STATIC=OFF \
        -DCMAKE_C_FLAGS=-DHAVE_GCC_FVISIBILITY=OFF\ "${CFLAGS}" && \
    make -j $(nproc) && make install
RUN cp -rf ${HOME}/src/SDL2-${SDL2_VERSION}/include/* /root/prefix/include/

# Setup BULLET
RUN wget -c https://github.com/bulletphysics/bullet3/archive/${BULLET_VERSION}.tar.gz -O - | tar -xz -C $HOME/src/ && \
    mkdir -p ${HOME}/src/bullet3-${BULLET_VERSION}/build && cd $_ && \
    cmake ${HOME}/src/bullet3-${BULLET_VERSION} \
        ${COMMON_CMAKE_ARGS} \
        -DBUILD_BULLET2_DEMOS=OFF \
        -DBUILD_CPU_DEMOS=OFF \
        -DBUILD_UNIT_TESTS=OFF \
        -DBUILD_EXTRAS=OFF \
        -DUSE_DOUBLE_PRECISION=ON \
        -DBULLET2_MULTITHREADING=ON && \
    make -j $(nproc) && make install

# Setup GL4ES_VERSION
RUN wget -c https://github.com/Duron27/gl4es/archive/refs/tags/${GL4ES_VERSION}.tar.gz -O - | tar -xz -C ${HOME}/src/ && \
    mkdir -p ${HOME}/src/gl4es-${GL4ES_VERSION}/build && cd $_ && \
    cmake ../ ${COMMON_CMAKE_ARGS} && \
    make -j $(nproc) && make install

# Setup MYGUI
RUN wget -c https://github.com/MyGUI/mygui/archive/MyGUI${MYGUI_VERSION}.tar.gz -O - | tar -xz -C $HOME/src/ && \
    mkdir -p ${HOME}/src/mygui-MyGUI${MYGUI_VERSION}/build && cd $_ && \
    cmake ${HOME}/src/mygui-MyGUI${MYGUI_VERSION} \
        ${COMMON_CMAKE_ARGS} \
        -DMYGUI_RENDERSYSTEM=1 \
        -DMYGUI_BUILD_DEMOS=OFF \
        -DMYGUI_BUILD_TOOLS=OFF \
        -DMYGUI_BUILD_PLUGINS=OFF \
        -DMYGUI_DONT_USE_OBSOLETE=ON \
        -DMYGUI_STATIC=ON && \
    make -j $(nproc) && make install

# Setup LZ4
RUN wget -c https://github.com/lz4/lz4/archive/v${LZ4_VERSION}.tar.gz -O - | tar -xz -C $HOME/src/ && \
    mkdir -p ${HOME}/src/lz4-${LZ4_VERSION}/build && cd $_ && \
    cmake ${HOME}/src/lz4-${LZ4_VERSION}/build/cmake/ \
        ${COMMON_CMAKE_ARGS} \
        -DBUILD_STATIC_LIBS=ON \
        -DBUILD_SHARED_LIBS=OFF && \
    make -j $(nproc) && make install

# Setup LUAJIT_VERSION
RUN wget -c https://github.com/luaJit/LuaJIT/archive/v${LUAJIT_VERSION}.tar.gz -O - | tar -xz -C ${HOME}/src/ && \
    cd ${HOME}/src/LuaJIT-${LUAJIT_VERSION} && \
    make amalg \
    HOST_CC='gcc -m64' \
    CFLAGS= \
    TARGET_CFLAGS="${CFLAGS}" \
    PREFIX=${PREFIX} \
    CROSS=${TOOLCHAIN}/bin/llvm- \
    STATIC_CC=${NDK_TRIPLET}${API}-clang \
    DYNAMIC_CC="${NDK_TRIPLET}${API}-clang -fPIC" \
    TARGET_LD=${NDK_TRIPLET}${API}-clang && \
    make install \
    HOST_CC='gcc -m64' \
    CFLAGS= \
    TARGET_CFLAGS="${CFLAGS}" \
    PREFIX=${PREFIX} \
    CROSS=${TOOLCHAIN}/bin/llvm- \
    STATIC_CC=${NDK_TRIPLET}${API}-clang \
    DYNAMIC_CC="${NDK_TRIPLET}${API}-clang -fPIC" \
    TARGET_LD=${NDK_TRIPLET}${API}-clang

RUN bash -c "rm ${PREFIX}/lib/libluajit*.so*"

# Setup LIBCOLLADA_VERSION
# The 3 sed commands are required for boost 1.85.0
RUN wget -c https://github.com/rdiankov/collada-dom/archive/v${COLLADA_DOM_VERSION}.tar.gz -O - | tar -xz -C ${HOME}/src/ && cd ${HOME}/src/collada-dom-${COLLADA_DOM_VERSION} && \
    cd ${HOME}/src/collada-dom-${COLLADA_DOM_VERSION} && \
    sed -i 's|#include <boost/filesystem/convenience.hpp>|#include <boost/filesystem.hpp>|g' dom/include/dae.h && \
    sed -i 's|#include <boost/filesystem/convenience.hpp>|#include <boost/filesystem.hpp>|g' dom/src/dae/daeUtils.cpp && \
    sed -i 's|std::string dir = archivePath.branch_path().string();|std::string dir = archivePath.parent_path().string();|g' dom/src/dae/daeZAEUncompressHandler.cpp && \
    mkdir -p ${HOME}/src/collada-dom-${COLLADA_DOM_VERSION}/build && cd $_ && \
    cmake .. \
        ${COMMON_CMAKE_ARGS} \
        -DBoost_USE_STATIC_LIBS=ON \
        -DBoost_USE_STATIC_RUNTIME=ON \
        -DBoost_NO_SYSTEM_PATHS=ON \
        -DBoost_INCLUDE_DIR=${PREFIX}/include \
        -DCMAKE_CXX_FLAGS=-Dauto_ptr=unique_ptr\ "${CXXFLAGS}" && \
    make -j $(nproc) && make install

# Setup Delta Plugin
RUN cd root/src && git clone https://gitlab.com/bmwinger/delta-plugin && cd delta-plugin && cargo build --target ${NDK_TRIPLET} --release
RUN cp /root/src/delta-plugin/target/${NDK_TRIPLET}/release/delta_plugin ${PREFIX}/lib/libdelta_plugin.so

# Setup OPENSCENEGRAPH_VERSION
RUN wget -c https://github.com/Duron27/osg/archive/refs/tags/${OSG_VERSION}.tar.gz -O - | tar -xz -C ${HOME}/src/ && \
    mkdir -p ${HOME}/src/osg-${OSG_VERSION}/build && cd $_ && \
    cmake .. \
        ${COMMON_CMAKE_ARGS} \
        -DOPENGL_PROFILE=GL1 \
        -DDYNAMIC_OPENTHREADS=OFF \
        -DDYNAMIC_OPENSCENEGRAPH=OFF \
        -DBUILD_OSG_PLUGIN_OSG=ON \
        -DBUILD_OSG_PLUGIN_DAE=ON \
        -DBUILD_OSG_PLUGIN_DDS=ON \
        -DBUILD_OSG_PLUGIN_TGA=ON \
        -DBUILD_OSG_PLUGIN_BMP=ON \
        -DBUILD_OSG_PLUGIN_JPEG=ON \
        -DBUILD_OSG_PLUGIN_PNG=ON \
        -DBUILD_OSG_PLUGIN_FREETYPE=ON \
        -DOSG_CPP_EXCEPTIONS_AVAILABLE=TRUE \
        -DJPEG_INCLUDE_DIR=${PREFIX}/include/ \
        -DPNG_INCLUDE_DIR=${PREFIX}/include/ \
        -DCOLLADA_INCLUDE_DIR=${PREFIX}/include/collada-dom2.5 \
        -DOSG_GL1_AVAILABLE=ON \
        -DOSG_GL2_AVAILABLE=OFF \
        -DOSG_GL3_AVAILABLE=OFF \
        -DOSG_GLES1_AVAILABLE=OFF \
        -DOSG_GLES2_AVAILABLE=OFF \
        -DOSG_GL_LIBRARY_STATIC=OFF \
        -DOSG_GL_DISPLAYLISTS_AVAILABLE=OFF \
        -DOSG_GL_MATRICES_AVAILABLE=ON \
        -DOSG_GL_VERTEX_FUNCS_AVAILABLE=ON \
        -DOSG_GL_VERTEX_ARRAY_FUNCS_AVAILABLE=ON \
        -DOSG_GL_FIXED_FUNCTION_AVAILABLE=ON \
        -DBUILD_OSG_APPLICATIONS=OFF \
        -DBUILD_OSG_PLUGINS_BY_DEFAULT=OFF \
        -DBUILD_OSG_DEPRECATED_SERIALIZERS=OFF \
        -DOSG_FIND_3RD_PARTY_DEPS=OFF \
        -DOPENGL_INCLUDE_DIR=${PREFIX}/include/gl4es/ \
        -DCMAKE_CXX_FLAGS=-Dauto_ptr=unique_ptr\ -I${PREFIX}/include/freetype2/\ "${CXXFLAGS}" && \
    make -j $(nproc) && make install

# Create a zip of all the libraries
#RUN cd /root/prefix && zip -r /openmw-android-deps.zip ./*

COPY --chmod=0755 patches /root/patches

# Setup OPENMW_VERSION
RUN wget -c https://github.com/OpenMW/openmw/archive/${OPENMW_VERSION}.tar.gz -O - | tar -xz -C ${HOME}/src/
RUN patch -d ${HOME}/src/openmw-${OPENMW_VERSION} -p1 -t -N < /root/patches/openmw/Settings_cfg_on_ok.patch
RUN patch -d ${HOME}/src/openmw-${OPENMW_VERSION} -p1 -t -N < /root/patches/openmw/0010-android-fix-context-being-lost-on-app-minimize.patch
RUN patch -d ${HOME}/src/openmw-${OPENMW_VERSION} -p1 -t -N < /root/patches/openmw/shaders.patch
RUN patch -d ${HOME}/src/openmw-${OPENMW_VERSION} -p1 -t -N < /root/patches/openmw/Post_additions.patch
RUN patch -d ${HOME}/src/openmw-${OPENMW_VERSION} -p1 -t -N < /root/patches/openmw/fix_shadows.patch
RUN patch -d ${HOME}/src/openmw-${OPENMW_VERSION} -p1 -t -N < /root/patches/openmw/fixnew.patch
RUN patch -d ${HOME}/src/openmw-${OPENMW_VERSION} -p1 -t -N < /root/patches/openmw/navmeshtool.patch
RUN cp /root/patches/openmw/android_main.cpp /root/src/openmw-${OPENMW_VERSION}/apps/openmw/android_main.cpp

# sed commands
# change post processing window size for android
RUN sed -i 's/600 600/600 400/g' ${HOME}/src/openmw-${OPENMW_VERSION}/files/data/mygui/openmw_postprocessor_hud.layout

RUN mkdir -p ${HOME}/src/openmw-${OPENMW_VERSION}/build && cd $_ && \
    cmake .. \
        ${COMMON_CMAKE_ARGS} \
        -DBUILD_BSATOOL=0 \
        -DBUILD_NIFTEST=0 \
        -DBUILD_ESMTOOL=0 \
        -DBUILD_LAUNCHER=0 \
        -DBUILD_MWINIIMPORTER=0 \
        -DBUILD_ESSIMPORTER=0 \
        -DBUILD_OPENCS=0 \
        -DBUILD_NAVMESHTOOL=0 \
        -DBUILD_WIZARD=0 \
        -DBUILD_MYGUI_PLUGIN=0 \
        -DOPENMW_GL4ES_MANUAL_INIT=ON \
        -DBUILD_BULLETOBJECTTOOL=0 \
        -DOPENMW_USE_SYSTEM_SQLITE3=OFF \
        -DOPENMW_USE_SYSTEM_YAML_CPP=OFF \
        -DOPENMW_USE_SYSTEM_ICU=ON \
        -DOPENAL_INCLUDE_DIR=${PREFIX}/include/AL/ \
        -DBullet_INCLUDE_DIR=${PREFIX}/include/bullet/ \
        -DOSG_STATIC=TRUE \
        -DCMAKE_CXX_FLAGS=-I${PREFIX}/include/\ "${CXXFLAGS}" \
        -DMyGUI_LIBRARY=${PREFIX}/lib/libMyGUIEngineStatic.a && \
    make -j $(nproc)

COPY --chmod=0755 payload /root/payload
COPY --chmod=0755 mods /root/mods

# Finalize
RUN rm -rf /root/payload/app/wrap/ && rm -rf /root/payload/app/src/main/jniLibs/${ABI}/ && mkdir -p /root/payload/app/src/main/jniLibs/${ABI}/

# libopenmw.so is a special case
RUN find /root/src/openmw-${OPENMW_VERSION}/ -iname "libopenmw.so" -exec cp "{}" /root/payload/app/src/main/jniLibs/${ABI}/libopenmw.so \;

# copy over libs we compiled
RUN cp ${PREFIX}/lib/{libopenal,libSDL2,libGL,libcollada-dom2.5-dp,libdelta_plugin}.so /root/payload/app/src/main/jniLibs/${ABI}/

# copy over libc++_shared
RUN find ${TOOLCHAIN}/sysroot/usr/lib/${NDK_TRIPLET} -iname "libc++_shared.so" -exec cp "{}" /root/payload/app/src/main/jniLibs/${ABI}/ \;
ENV DST=/root/payload/app/src/main/assets/libopenmw/
ENV SRC=/root/src/openmw-${OPENMW_VERSION}/build/
RUN rm -rf "${DST}" && mkdir -p "${DST}"

# Copy over Resources
RUN cp -r "${SRC}/resources" "${DST}"

# Copy over Mods
RUN cd ${DST}/resources/vfs/ && cp -r /root/mods/* .

# Global Config
RUN mkdir -p "${DST}/openmw/"
RUN cp "${SRC}/defaults.bin" "${DST}/openmw/"
RUN cp "${SRC}/gamecontrollerdb.txt" "${DST}/openmw/"
RUN cat "${SRC}/openmw.cfg" | grep -v "data=" | grep -v "data-local=" >> "${DST}/openmw/openmw.base.cfg"
RUN cat "/root/payload/app/openmw.base.cfg" >> "${DST}/openmw/openmw.base.cfg"
RUN mkdir -p /root/payload/app/src/main/assets/libopenmw/resources && cd $_ && echo "${APP_VERSION}" >> version
RUN sed -i "4i\    <string name='version_info'>CaveBros Version ${APP_VERSION}</string>" /root/payload/app/src/main/res/values/strings.xml
RUN sed -i "92i\    ndkVersion \"${NDK_VERSION}\"" /root/payload/app/build.gradle

# licensing info
RUN cp "/root/payload/3rdparty-licenses.txt" "${DST}"

# Remove Debug Symbols
RUN llvm-strip /root/payload/app/src/main/jniLibs/arm64-v8a/*.so

# Create Package for external Editing
RUN zip -r Package.zip /root/payload

# Build the APK!
RUN alternatives --set java java-11-openjdk.x86_64
RUN JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which java)))))
RUN cd /root/payload/ && ./gradlew assembleRelease

RUN cp /root/payload/app/build/outputs/apk/mainline/release/*.apk openmw-android.apk