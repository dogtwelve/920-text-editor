#!/bin/bash

mkdir tmp
cp lang.conf tmp/
cp -rf syntax tmp/
rm -rf tmp/syntax/.svn
cd tmp
zip -r syntax.zip .
rm -rf ../../assets/syntax.zip
mv syntax.zip ../../assets/
cd ..
rm -rf tmp
