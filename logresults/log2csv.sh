#!/bin/bash
# Takes two parameters
# $1 File to read data from
# $2 File to write to. NOTE: this file will be overwritten
touch "$2"
echo "step,money" > $2

touch temp.csv
cat $1 >> temp.csv
sed -i -e 's/Step: //g' temp.csv
sed -i -e 's/ - Money: /,/g' temp.csv
sed -i -e 's/999/1000/g' temp.csv
sed -i -i 's/^1,/0,/g' temp.csv
grep -e '^[0-9]*00' -e '^[0-9]*50,' -e '^999' -e '^0' temp.csv >> $2
rm temp.csv
