#! /bin/sh

i=1

while [ $i -lt 101 ]
do
	touch ./nodeFile/File$i
	i=$((i+1))

	echo $i
done