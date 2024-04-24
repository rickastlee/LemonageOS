#!/usr/bin/bash

BOLD="\033[1m"
END="\033[0m"
RED="\033[91m"

WORKDIR="/home/sanu/rickastlee"
LEMONAGE="$WORKDIR/LemonageOS"
LINEAGE="$WORKDIR/android/lineage"
PYTHON="$LEMONAGE/scripts/python"
LOGDIR="$LEMONAGE/build logs"

MAKEFILE="$LINEAGE/lineage_miatoll.mk"
LOGFILE="$LOGDIR/$(date +%Y-%m-%d\ %H:%M)"

if [ -f $MAKEFILE ]; then
    printf "${BOLD}${RED}Not proceeding to build LineageOS. Please revert local commits first${END}\n"
    exit 1
fi

if [ ! -d $LOGDIR ]; then
    mkdir -p "$LOGDIR"
fi

cd $PYTHON
python3 merge_commits.py

cd $LINEAGE
source build/envsetup.sh
lunch lineage_miatoll-userdebug
mka bacon 2>&1 | tee "$LOGFILE"

cd $PYTHON
python3 revert_commits.py
