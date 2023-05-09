#!/usr/bin/env bash

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

 java -cp "$SCRIPTPATH/../bin/jar/STL2TTML.jar" au.com.foxtel.product.subtitleTools.ConvertSTL "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8" "$9"