#!/usr/bin/env bash

set -e

GIT_STABLE_COMMIT=${1:-"HEAD"}
GIT_STABLE_TAG=stable

git tag -d ${GIT_STABLE_TAG}
git tag -a ${GIT_STABLE_TAG} -m 'Stable Release' ${GIT_STABLE_COMMIT}

echo ""

while true; do
    read -p "Do you want to publish this release? [Y/n] " yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done

git push --delete origin ${GIT_STABLE_TAG}
git push --tags origin master
