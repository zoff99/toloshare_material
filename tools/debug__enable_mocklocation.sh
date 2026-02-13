#! /bin/bash

_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"

f1="src/main/kotlin/ToLoShareMain.kt"
f2="src/main/kotlin/OsmVM.kt"

cd "$basedir"


sed -i -e 's#val ___MOCK_FRIEND_LOCATION___ = false#val ___MOCK_FRIEND_LOCATION___ = true#g' "$f1"
sed -i -e 's#INITIAL_ZOOM_LEVEL = 5.0#INITIAL_ZOOM_LEVEL = 3.0#g' "$f2"


