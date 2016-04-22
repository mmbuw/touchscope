#!/bin/bash
cd ~/Android/Sdk/platform-tools/
read -p "connect device and press enter" 
./adb tcpip 5555
if [ "$?" -ne "0" ]; then
  echo "could not connect device"
  exit 1
fi
read -p "disconnect device and press enter"
./adb connect 10.0.1.6:5555
if [ "$?" -ne "0" ]; then
  echo "could not find device on wifi"
fi
