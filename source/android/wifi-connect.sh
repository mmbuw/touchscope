#!/bin/bash

function stop_adb
{
  ./adb kill-server
}

cd ~/Android/Sdk/platform-tools/
read -p "connect device and press enter" 
./adb tcpip 5555
if [ "$?" -ne "0" ]; then
  stop_adb
  echo "could not connect device"
  exit 1
fi

sleep 5

ip=$(./adb shell ip route | cut -d " " -f12)
echo "attempt to connect to $ip"

read -p "disconnect device and press enter"

./adb connect $ip:5555
if [ "$?" -ne "0" ]; then
  stop_adb
  echo "could not find device on wifi"
fi
