#!/bin/bash

wget https://github.com/scgray/jsqsh/archive/jsqsh-2.1.1.tar.gz
tar -xvzf jsqsh-2.1.1.tar.gz
rm jsqsh-2.1.1.tar.gz
mv jsqsh-jsqsh-2.1.1 jsqsh-2.1.1.orig 
cd jsqsh-2.1.1.orig
rm -rf lib-nondist
rm -rf dist/win32/JavaGetline.dll
rm -rf dist/archive
rm -rf src/main/resources/org/sqsh/jni
cd -
tar -cvzf jsqsh_2.1.1+ds.orig.tar.gz jsqsh-2.1.1.orig
rm -rf jsqsh-2.1.1.orig
