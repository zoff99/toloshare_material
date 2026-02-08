#! /bin/bash

_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"

f1="src/main/kotlin/ToLoShareMain.kt"

cd "$basedir"


sed -i -e 's#val ___MOCK_FRIEND_LOCATION___ = false#val ___MOCK_FRIEND_LOCATION___ = true#g' "$f1"


