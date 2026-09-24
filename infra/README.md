# AWS CloudFormation Deployment Guide

Deploy Spring Boot backend to AWS ap-southeast-1 with ALB, EC2, RDS PostgreSQL, and Redis.

This directory is the repo-root `infra/` folder — a sibling of `backend/` and
`frontend/`, not part of either app. All commands below are run from `infra/`.

## Quick Start (5 Minutes)

```bash
cd infra

# 1. Get your VPC details
./get-vpc-info.sh vpc-YOUR_VPC_ID

# 2. Deploy everything
./deploy.sh
```

The script will prompt for VPC IDs, passwords, and deploy the entire stack.

---

## Architecture

```
Internet → ALB (HTTP:80) → EC2 (8080) → RDS PostgreSQL + Redis
           (public)         (public)     (private subnets)
```

**Components:**

- Application Load Balancer (internet-facing)
- EC2 instance (Amazon Linux 2023 + Java 25)
- RDS PostgreSQL (encrypted, automated backups)
- ElastiCache Redis (session storage)
- Security groups with proper isolation

---

## Prerequisites

1. **Existing VPC in ap-southeast-1** with:
   - 2 public subnets (different AZs) for ALB
   - 2 private subnets (different AZs) for RDS/Redis
   - Internet Gateway attached

2. **AWS CLI** configured:

   ```bash
   aws configure
   # Set region: ap-southeast-1
   ```

3. **Application built** — the *integrated* JAR (SPA + backend), from the repo
   root:

   ```bash
   make package     # == scripts/package.sh
   ```

   `./mvnw clean package` in `backend/` is a **backend-only** build: the
   `with-frontend` profile is off by default, so that JAR serves no SPA.
   `make package` builds `frontend/dist`, activates the profile with an explicit
   `-Dfrontend.dist.dir`, and fails if `BOOT-INF/classes/static/index.html` is
   missing from the artefact. `./deploy.sh` runs this for you when you answer yes
   to the JAR-deployment prompt.

---

## Deployment

### Option 1: Automated (Recommended)

```bash
./deploy.sh
```

### Option 2: Manual

```bash
# 1. Get VPC info
./get-vpc-info.sh vpc-xxxxx

# 2. Configure parameters
cp parameters.template.json parameters.json
# Edit parameters.json with your VPC IDs and passwords

# 3. Deploy stack
aws cloudformation create-stack \
  --stack-name spring-backend \
  --template-body file://infrastructure.yaml \
  --parameters file://parameters.json \
  --capabilities CAPABILITY_NAMED_IAM \
  --region ap-southeast-1

# 4. Wait for completion (15-20 minutes)
aws cloudformation wait stack-create-complete \
  --stack-name spring-backend \
  --region ap-southeast-1
```

---

## SSH Key Management

### CloudFormation Creates Key (Recommended)

Set in `parameters.json`:

```json
{
  "ParameterKey": "CreateKeyPair",
  "ParameterValue": "true"
}
```

**Retrieve private key after deployment:**

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

### Use Existing Key

Set in `parameters.json`:

```json
{
  "ParameterKey": "CreateKeyPair",
  "ParameterValue": "false"
},
{
  "ParameterKey": "KeyName",
  "ParameterValue": "my-existing-key"
}
```

---

## Deploy Application

```bash
# Get EC2 IP
EC2_IP=$(aws cloudformation describe-stacks \
  --stack-name spring-backend \
  --region ap-southeast-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`EC2PublicIP`].OutputValue' \
  --output text)

# Copy JAR (paths assume you're in the infra/ directory)
scp -i spring-backend-key.pem \
  ../backend/target/backend-0.0.1-SNAPSHOT.jar \
  ec2-user@$EC2_IP:/tmp/backend.jar

# Start application
ssh -i spring-backend-key.pem ec2-user@$EC2_IP << 'ENDSSH'
sudo mv /tmp/backend.jar /opt/backend/backend.jar
sudo chown springboot:springboot /opt/backend/backend.jar
sudo systemctl start backend
sudo systemctl enable backend
ENDSSH
```

---

## Test Application

```bash
# Get ALB URL
ALB_URL=$(aws cloudformation describe-stacks \
  --stack-name spring-backend \
  --region ap-southeast-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerURL`].OutputValue' \
  --output text)

# Test health
curl $ALB_URL/actuator/health

# Login
curl -X POST $ALB_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"YOUR_PASSWORD"}' \
  -c cookies.txt

# Test authenticated endpoint
curl $ALB_URL/api/count -b cookies.txt
```

---

## Parameters Reference

| Parameter            | Description        | Required | Default            |
| -------------------- | ------------------ | -------- | ------------------ |
| VpcId                | Existing VPC ID    | Yes      | -                  |
| PublicSubnet1Id      | Public subnet 1    | Yes      | -                  |
| PublicSubnet2Id      | Public subnet 2    | Yes      | -                  |
| PrivateSubnet1Id     | Private subnet 1   | Yes      | -                  |
| PrivateSubnet2Id     | Private subnet 2   | Yes      | -                  |
| CreateKeyPair        | Create key pair    | No       | true               |
| KeyName              | Key pair name      | Yes      | spring-backend-key |
| InstanceType         | EC2 type           | No       | t3.small           |
| DBPassword           | Database password  | Yes      | -                  |
| RedisPassword        | Redis password     | No       | (empty)            |
| AppPassword          | Admin password     | Yes      | -                  |
| AppSecondaryPassword | Secondary password | Yes      | -                  |

---

## Common Tasks

### View Logs

```bash
ssh -i spring-backend-key.pem ec2-user@$EC2_IP
sudo journalctl -u backend -f
```

### Restart Application

```bash
ssh -i spring-backend-key.pem ec2-user@$EC2_IP "sudo systemctl restart backend"
```

### Update Application

```bash
# Build the integrated JAR (SPA + backend), from the repo root
(cd .. && make package)

# Confirm the SPA is in the artefact (package.sh already asserts this)
unzip -Z1 ../backend/target/backend-0.0.1-SNAPSHOT.jar BOOT-INF/classes/static/index.html

# Copy and restart (from infra/)
scp -i spring-backend-key.pem \
  ../backend/target/backend-0.0.1-SNAPSHOT.jar \
  ec2-user@$EC2_IP:/tmp/backend.jar

ssh -i spring-backend-key.pem ec2-user@$EC2_IP << 'ENDSSH'
sudo systemctl stop backend
sudo mv /tmp/backend.jar /opt/backend/backend.jar
sudo chown springboot:springboot /opt/backend/backend.jar
sudo systemctl start backend
ENDSSH
```

### Check ALB Target Health

```bash
TG_ARN=$(aws elbv2 describe-target-groups \
  --names spring-backend-TG \
  --region ap-southeast-1 \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text)

aws elbv2 describe-target-health \
  --target-group-arn $TG_ARN \
  --region ap-southeast-1
```

### Delete Stack

```bash
./cleanup.sh

# Or manually:
aws cloudformation delete-stack \
  --stack-name spring-backend \
  --region ap-southeast-1
```

### Inspect the Stack

```bash
aws cloudformation describe-stack-events --stack-name spring-backend \
  --region ap-southeast-1 --max-items 20

aws cloudformation describe-stacks --stack-name spring-backend \
  --region ap-southeast-1 --query 'Stacks[0].Outputs' --output table
```

---

## Troubleshooting

### Health Check Failing

Check application status:

```bash
ssh -i spring-backend-key.pem ec2-user@$EC2_IP
sudo systemctl status backend
sudo journalctl -u backend -n 100
```

Check target health:

```bash
aws elbv2 describe-target-health --target-group-arn $TG_ARN
```

**Common causes:**

- Application not running: `sudo systemctl start backend`
- Wrong health check path (should be `/actuator/health`)
- Security group blocking ALB → EC2:8080

### Can't Access via ALB

1. Check ALB DNS is correct
2. Verify security groups:
   - ALB SG allows 80 from internet
   - EC2 SG allows 8080 from ALB SG
3. Check target group shows "healthy"

### Database Connection Error

```bash
ssh -i spring-backend-key.pem ec2-user@$EC2_IP

# Check environment
sudo cat /opt/backend/.env | grep DATABASE

# Test connection
sudo dnf install -y postgresql15
psql -h RDS_ENDPOINT -U backend -d backend
```

### Can't Retrieve SSH Key

- Verify `CreateKeyPair: true` in parameters
- Key only retrievable once from SSM - store it safely
- If lost, deploy new stack or use existing key

---

`QUICKSTART.md` beside this file is the condensed, command-only walkthrough of
the same deployment.
