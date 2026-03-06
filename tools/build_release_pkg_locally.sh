#! /bin/bash

_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"

cd "$basedir"

./gradlew packageReleaseDeb

out_pkg=$(ls -1tr ./build/compose/binaries/main-release/deb/toloshare-material*_amd64.deb 2>/dev/null | tail -1 2> /dev/null)
echo "found $out_pkg"
if [ "$out_pkg""x" == "x" ]; then
    echo "pkg not found"
    exit 1
else
    cp -v "$out_pkg" aa.deb
    tools/fix_debian_pkg.sh aa.deb
    rm -f aa.deb
fi
