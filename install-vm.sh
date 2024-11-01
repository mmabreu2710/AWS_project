#!/bin/bash

source config.sh

# Install java.
cmd="sudo yum update -y; sudo yum install java-11-amazon-corretto.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# Install web server.
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH -r $DIR/ ec2-user@$(cat instance.dns):

# Build web server.
#cmd="mvn clean package"
#ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd 

# Build javassist tools.
#cmd="mvn -f javassist clean package"
#ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd 

#Install aws zip
cmd="curl "http://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip" -o "cnv24-g28-copia/aws-java-sdk.zip""
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

#unzip aws zip
cmd="unzip cnv24-g28-copia/aws-java-sdk.zip -d cnv24-g28-copia"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# Setup web server to start on instance launch.
cmd="echo \"bash /home/ec2-user/cnv24-g28-copia/instrument.sh\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd