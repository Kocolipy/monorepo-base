# Quick Start

## 1. Get VPC Info

```bash
./get-vpc-info.sh vpc-YOUR_VPC_ID
```

## 2. Deploy

```bash
./deploy.sh
```

## 3. Get Private Key (if CloudFormation created it)

```bash
KEY_PAIR_ID=$(aws cloudformation describe-stacks \
  --stack-name spring-backend \
  --region ap-southeast-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`KeyPairId`].OutputValue' \
  --output text)

aws ssm get-parameter \
  --name /ec2/keypair/${KEY_PAIR_ID} \
  --with-decryption \
  --query Parameter.Value \
  --output text > spring-backend-key.pem

chmod 400 spring-backend-key.pem
```

## 4. Deploy Application

```bash
# From cloudformation/ directory
EC2_IP=$(aws cloudformation describe-stacks \
  --stack-name spring-backend \
  --region ap-southeast-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`EC2PublicIP`].OutputValue' \
  --output text)

scp -i spring-backend-key.pem \
  ../target/backend-0.0.1-SNAPSHOT.jar \
  ec2-user@$EC2_IP:/tmp/backend.jar

ssh -i spring-backend-key.pem ec2-user@$EC2_IP << 'ENDSSH'
sudo mv /tmp/backend.jar /opt/backend/backend.jar
sudo chown springboot:springboot /opt/backend/backend.jar
sudo systemctl start backend
sudo systemctl enable backend
ENDSSH
```

## 5. Test

```bash
ALB_URL=$(aws cloudformation describe-stacks \
  --stack-name spring-backend \
  --region ap-southeast-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerURL`].OutputValue' \
  --output text)

curl $ALB_URL/actuator/health
```

---

**For detailed documentation, see [README.md](./README.md)**
