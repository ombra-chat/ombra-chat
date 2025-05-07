#!/bin/sh

#
# Ugly workaround to patch the twelvemonkeys jar adding module-info.java
# Needed to create a working jlink image
#

# exit on error
set -e

export VERSION="3.12.0"

cd lib/modules/twelvemonkeys

cp common-lang/module-info.java ~/.m2/repository/com/twelvemonkeys/common/common-lang/$VERSION
cp imageio-core/module-info.java ~/.m2/repository/com/twelvemonkeys/imageio/imageio-core/$VERSION
cp imageio-metadata/module-info.java ~/.m2/repository/com/twelvemonkeys/imageio/imageio-metadata/$VERSION
cp imageio-webp/module-info.java ~/.m2/repository/com/twelvemonkeys/imageio/imageio-webp/$VERSION

cd ~/.m2/repository/com/twelvemonkeys/common/common-lang/$VERSION
javac --patch-module com.twelvemonkeys.common.lang=common-lang-$VERSION.jar module-info.java
jar uf common-lang-$VERSION.jar module-info.class

cd ~/.m2/repository/com/twelvemonkeys/imageio/imageio-metadata/$VERSION
javac --patch-module com.twelvemonkeys.imageio.metadata=imageio-metadata-$VERSION.jar module-info.java
jar uf imageio-metadata-$VERSION.jar module-info.class

cd ~/.m2/repository/com/twelvemonkeys/imageio/imageio-core/$VERSION
javac \
  --module-path=../../../common/common-lang/$VERSION/common-lang-$VERSION.jar \
  --patch-module com.twelvemonkeys.imageio.core=imageio-core-$VERSION.jar module-info.java
jar uf imageio-core-$VERSION.jar module-info.class

cd ~/.m2/repository/com/twelvemonkeys/imageio/imageio-webp/$VERSION
javac \
  --module-path=../../../common/common-lang/$VERSION/common-lang-$VERSION.jar:../../imageio-core/$VERSION/imageio-core-$VERSION.jar \
  --patch-module com.twelvemonkeys.imageio.webp=imageio-webp-$VERSION.jar module-info.java
jar uf imageio-webp-$VERSION.jar module-info.class
