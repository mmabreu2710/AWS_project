#!/bin/bash

source config.sh

# Requesting an instance reboot.
aws ec2 reboot-instances --instance-ids $(cat instance.id)
echo "Rebooting instance to test web server auto-start."

# Letting the instance shutdown.
sleep 1

# Wait for port 8000 to become available.
while ! nc -z $(cat instance.dns) 8000; do
	echo "Waiting for $(cat instance.dns):8000..."
	sleep 0.5
done

# Sending a query!
echo "Sending a query!"

base64 imageproc/resources/airplane.jpg > temp.txt
echo -e "data:image/jpg;base64,$(cat temp.txt)" > temp.txt
curl -X POST http://$(cat instance.dns):8000/blurimage --data @"./temp.txt" > result.txt
sed -i 's/^[^,]*,//' result.txt
base64 -d result.txt > result.jpg
