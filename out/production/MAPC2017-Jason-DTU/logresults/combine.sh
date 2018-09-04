pr -m -t -s, Solution*.csv | sed -e 's/,/ /g' | awk '{print $1,$2,$5}' | sed -e 's/ /,/g' > final.csv
sed -i -e 's/money,money/first,third/g' final.csv
