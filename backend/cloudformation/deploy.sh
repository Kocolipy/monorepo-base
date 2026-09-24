#!/bin/bash

# AWS CloudFormation Deployment Script for Spring Boot Backend

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."
    
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed. Please install it first."
        exit 1
    fi
    
    if ! aws sts get-caller-identity &> /dev/null; then
        print_error "AWS CLI is not configured. Run 'aws configure' first."
        exit 1
    fi
    
    print_info "Prerequisites check passed"
}

# Get user inputs
get_inputs() {
    print_info "CloudFormation Stack Configuration"
    echo
    
    read -p "Enter stack name (default: spring-backend): " STACK_NAME
    STACK_NAME=${STACK_NAME:-spring-backend}
    
    AWS_REGION="ap-southeast-1"
    print_info "Region set to: ap-southeast-1 (Singapore)"
    
    # VPC Configuration (Required)
    echo
    print_info "VPC Configuration (Existing VPC Required)"
    
    read -p "Enter existing VPC ID (e.g., vpc-12345678): " VPC_ID
    if [ -z "$VPC_ID" ]; then
        print_error "VPC ID is required"
        exit 1
    fi
    
    read -p "Enter Public Subnet 1 ID (e.g., subnet-12345678): " PUBLIC_SUBNET_1_ID
    if [ -z "$PUBLIC_SUBNET_1_ID" ]; then
        print_error "Public Subnet 1 ID is required"
        exit 1
    fi
    
    read -p "Enter Public Subnet 2 ID (e.g., subnet-87654321): " PUBLIC_SUBNET_2_ID
    if [ -z "$PUBLIC_SUBNET_2_ID" ]; then
        print_error "Public Subnet 2 ID is required"
        exit 1
    fi
    
    read -p "Enter Private Subnet 1 ID (e.g., subnet-11111111): " PRIVATE_SUBNET_1_ID
    if [ -z "$PRIVATE_SUBNET_1_ID" ]; then
        print_error "Private Subnet 1 ID is required"
        exit 1
    fi
    
    read -p "Enter Private Subnet 2 ID (e.g., subnet-22222222): " PRIVATE_SUBNET_2_ID
    if [ -z "$PRIVATE_SUBNET_2_ID" ]; then
        print_error "Private Subnet 2 ID is required"
        exit 1
    fi
    
    echo
    # EC2 Key Pair Configuration
    print_info "EC2 Key Pair Configuration"
    read -p "Create new EC2 Key Pair? (y/n, default: y): " CREATE_KEY_PAIR
    CREATE_KEY_PAIR=${CREATE_KEY_PAIR:-y}
    
    if [[ $CREATE_KEY_PAIR =~ ^[Yy]$ ]]; then
        CREATE_KEY_PAIR_VALUE="true"
        read -p "Enter Key Pair name (default: spring-backend-key): " KEY_NAME
        KEY_NAME=${KEY_NAME:-spring-backend-key}
        print_info "CloudFormation will create the key pair. Retrieve private key after deployment."
    else
        CREATE_KEY_PAIR_VALUE="false"
        read -p "Enter existing Key Pair name: " KEY_NAME
        if [ -z "$KEY_NAME" ]; then
            print_error "Key Pair name is required"
            exit 1
        fi
    fi
    
    echo
    read -p "Enter EC2 instance type (default: t3.small): " INSTANCE_TYPE
    INSTANCE_TYPE=${INSTANCE_TYPE:-t3.small}
    if [ -z "$KEY_NAME" ]; then
        print_error "Key pair name is required"
        exit 1
    fi
    
    read -p "Enter EC2 instance type (default: t3.small): " INSTANCE_TYPE
    INSTANCE_TYPE=${INSTANCE_TYPE:-t3.small}
    
    read -p "Enter your IP for SSH access (e.g., 1.2.3.4/32): " SSH_LOCATION
    SSH_LOCATION=${SSH_LOCATION:-0.0.0.0/0}
    
    # Database configuration
    print_info "Database Configuration"
    read -p "Enter database name (default: backend): " DB_NAME
    DB_NAME=${DB_NAME:-backend}
    
    read -p "Enter database username (default: backend): " DB_USERNAME
    DB_USERNAME=${DB_USERNAME:-backend}
    
    read -sp "Enter database password: " DB_PASSWORD
    echo
    if [ -z "$DB_PASSWORD" ]; then
        print_error "Database password is required"
        exit 1
    fi
    
    read -p "Enter RDS instance class (default: db.t3.micro): " DB_INSTANCE_CLASS
    DB_INSTANCE_CLASS=${DB_INSTANCE_CLASS:-db.t3.micro}
    
    # Redis configuration
    print_info "Redis Configuration"
    read -p "Enter Redis node type (default: cache.t3.micro): " REDIS_NODE_TYPE
    REDIS_NODE_TYPE=${REDIS_NODE_TYPE:-cache.t3.micro}
    
    read -sp "Enter Redis password (leave empty for no auth): " REDIS_PASSWORD
    echo
    
    # Application configuration
    print_info "Application Configuration"
    read -p "Enter application admin username (default: admin): " APP_USERNAME
    APP_USERNAME=${APP_USERNAME:-admin}
    
    read -sp "Enter application admin password: " APP_PASSWORD
    echo
    if [ -z "$APP_PASSWORD" ]; then
        print_error "Application password is required"
        exit 1
    fi
    
    read -p "Enter secondary username (default: admin2): " APP_SECONDARY_USERNAME
    APP_SECONDARY_USERNAME=${APP_SECONDARY_USERNAME:-admin2}
    
    read -sp "Enter secondary password: " APP_SECONDARY_PASSWORD
    echo
    if [ -z "$APP_SECONDARY_PASSWORD" ]; then
        print_error "Secondary password is required"
        exit 1
    fi
}

# Create parameters file
create_parameters_file() {
    print_info "Creating parameters file..."
    
    cat > /tmp/cfn-parameters-${STACK_NAME}.json << EOF
[
  {
    "ParameterKey": "VpcId",
    "ParameterValue": "${VPC_ID}"
  },
  {
    "ParameterKey": "PublicSubnet1Id",
    "ParameterValue": "${PUBLIC_SUBNET_1_ID}"
  },
  {
    "ParameterKey": "PublicSubnet2Id",
    "ParameterValue": "${PUBLIC_SUBNET_2_ID}"
  },
  {
    "ParameterKey": "PrivateSubnet1Id",
    "ParameterValue": "${PRIVATE_SUBNET_1_ID}"
  },
  {
    "ParameterKey": "PrivateSubnet2Id",
    "ParameterValue": "${PRIVATE_SUBNET_2_ID}"
  },
  {
    "ParameterKey": "CreateKeyPair",
    "ParameterValue": "${CREATE_KEY_PAIR_VALUE}"
  },
  {
    "ParameterKey": "KeyName",
    "ParameterValue": "${KEY_NAME}"
  },
  {
    "ParameterKey": "InstanceType",
    "ParameterValue": "${INSTANCE_TYPE}"
  },
  {
    "ParameterKey": "SSHLocation",
    "ParameterValue": "${SSH_LOCATION}"
  },
  {
    "ParameterKey": "DBName",
    "ParameterValue": "${DB_NAME}"
  },
  {
    "ParameterKey": "DBUsername",
    "ParameterValue": "${DB_USERNAME}"
  },
  {
    "ParameterKey": "DBPassword",
    "ParameterValue": "${DB_PASSWORD}"
  },
  {
    "ParameterKey": "DBInstanceClass",
    "ParameterValue": "${DB_INSTANCE_CLASS}"
  },
  {
    "ParameterKey": "RedisNodeType",
    "ParameterValue": "${REDIS_NODE_TYPE}"
  },
  {
    "ParameterKey": "RedisPassword",
    "ParameterValue": "${REDIS_PASSWORD}"
  },
  {
    "ParameterKey": "AppUsername",
    "ParameterValue": "${APP_USERNAME}"
  },
  {
    "ParameterKey": "AppPassword",
    "ParameterValue": "${APP_PASSWORD}"
  },
  {
    "ParameterKey": "AppSecondaryUsername",
    "ParameterValue": "${APP_SECONDARY_USERNAME}"
  },
  {
    "ParameterKey": "AppSecondaryPassword",
    "ParameterValue": "${APP_SECONDARY_PASSWORD}"
  }
]
EOF
    
    print_info "Parameters file created at /tmp/cfn-parameters-${STACK_NAME}.json"
}

# Deploy stack
deploy_stack() {
    print_info "Deploying CloudFormation stack: ${STACK_NAME}"
    
    aws cloudformation create-stack \
        --stack-name ${STACK_NAME} \
        --template-body file://infrastructure.yaml \
        --parameters file:///tmp/cfn-parameters-${STACK_NAME}.json \
        --capabilities CAPABILITY_NAMED_IAM \
        --region ${AWS_REGION}
    
    print_info "Stack creation initiated. Waiting for completion..."
    print_warn "This may take 15-20 minutes..."
    
    aws cloudformation wait stack-create-complete \
        --stack-name ${STACK_NAME} \
        --region ${AWS_REGION}
    
    print_info "Stack created successfully!"
}

# Display outputs
display_outputs() {
    print_info "Retrieving stack outputs..."
    
    OUTPUTS=$(aws cloudformation describe-stacks \
        --stack-name ${STACK_NAME} \
        --region ${AWS_REGION} \
        --query 'Stacks[0].Outputs' \
        --output json)
    
    EC2_IP=$(echo $OUTPUTS | jq -r '.[] | select(.OutputKey=="EC2PublicIP") | .OutputValue')
    ALB_DNS=$(echo $OUTPUTS | jq -r '.[] | select(.OutputKey=="LoadBalancerDNS") | .OutputValue')
    ALB_URL=$(echo $OUTPUTS | jq -r '.[] | select(.OutputKey=="LoadBalancerURL") | .OutputValue')
    DB_ENDPOINT=$(echo $OUTPUTS | jq -r '.[] | select(.OutputKey=="DBEndpoint") | .OutputValue')
    REDIS_ENDPOINT=$(echo $OUTPUTS | jq -r '.[] | select(.OutputKey=="RedisEndpoint") | .OutputValue')
    SSH_COMMAND=$(echo $OUTPUTS | jq -r '.[] | select(.OutputKey=="SSHCommand") | .OutputValue')
    KEY_RETRIEVE_CMD=$(echo $OUTPUTS | jq -r '.[] | select(.OutputKey=="GetPrivateKeyCommand") | .OutputValue')
    
    echo
    print_info "=== Deployment Complete ==="
    echo
    echo "Load Balancer DNS: ${ALB_DNS}"
    echo "Application URL: ${ALB_URL}"
    echo "EC2 Instance IP (SSH): ${EC2_IP}"
    echo "Database Endpoint: ${DB_ENDPOINT}"
    echo "Redis Endpoint: ${REDIS_ENDPOINT}"
    echo
    
    if [ "$CREATE_KEY_PAIR_VALUE" = "true" ]; then
        echo "=== Retrieve Private Key ==="
        echo "Run this command to get your private key:"
        echo "${KEY_RETRIEVE_CMD}"
        echo
    fi
    
    echo "SSH Command:"
    echo "  ${SSH_COMMAND}"
    echo
    
    # Save to file
    cat > ${STACK_NAME}-outputs.txt << EOF
Stack Name: ${STACK_NAME}
Region: ${AWS_REGION}
Deployed: $(date)

Load Balancer DNS: ${ALB_DNS}
Application URL: ${ALB_URL}
EC2 Instance IP: ${EC2_IP}
Database Endpoint: ${DB_ENDPOINT}
Redis Endpoint: ${REDIS_ENDPOINT}

$(if [ "$CREATE_KEY_PAIR_VALUE" = "true" ]; then
echo "Retrieve Private Key Command:"
echo "${KEY_RETRIEVE_CMD}"
echo ""
fi)

SSH Command:
${SSH_COMMAND}

Next Steps:
1. $(if [ "$CREATE_KEY_PAIR_VALUE" = "true" ]; then echo "Retrieve private key using command above"; else echo "Use existing key: ${KEY_NAME}.pem"; fi)
2. Build your application: mvn clean package -DskipTests
3. Copy JAR to EC2: scp -i ${KEY_NAME}.pem ../target/backend-0.0.1-SNAPSHOT.jar ec2-user@${EC2_IP}:/tmp/backend.jar
4. SSH to EC2: ${SSH_COMMAND}
5. Move JAR: sudo mv /tmp/backend.jar /opt/backend/backend.jar && sudo chown springboot:springboot /opt/backend/backend.jar
6. Start service: sudo systemctl start backend && sudo systemctl enable backend
7. Check logs: sudo journalctl -u backend -f
8. Test via ALB: curl ${ALB_URL}/actuator/health
EOF
    
    print_info "Outputs saved to ${STACK_NAME}-outputs.txt"
    
    echo
    echo "=========================================="
    echo "Next Steps:"
    echo "=========================================="
    echo "1. Review outputs in ${STACK_NAME}-outputs.txt"
    if [ "$CREATE_KEY_PAIR_VALUE" = "true" ]; then
        echo "2. Retrieve your private key (see command above)"
    fi
    echo "3. Deploy your application JAR"
    echo "4. Test via ALB URL: ${ALB_URL}"
    echo
}

# Prompt for JAR deployment
prompt_jar_deployment() {
    echo
    read -p "Do you want to deploy the application JAR now? (y/n): " DEPLOY_JAR
    
    if [[ $DEPLOY_JAR =~ ^[Yy]$ ]]; then
        deploy_jar
    else
        print_warn "Skipping JAR deployment. Deploy manually using the instructions in ${STACK_NAME}-outputs.txt"
    fi
}

# Deploy JAR
deploy_jar() {
    print_info "Building application..."
    
    cd ..
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found. Run this script from the cloudformation directory."
        exit 1
    fi
    
    mvn clean package -DskipTests
    
    JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -n 1)
    
    if [ -z "$JAR_FILE" ]; then
        print_error "JAR file not found in target directory"
        exit 1
    fi
    
    print_info "Copying JAR to EC2 instance..."
    
    scp -i ${KEY_NAME}.pem \
        -o StrictHostKeyChecking=no \
        ${JAR_FILE} \
        ec2-user@${EC2_IP}:/tmp/backend.jar
    
    print_info "Configuring and starting application..."
    
    ssh -i ${KEY_NAME}.pem \
        -o StrictHostKeyChecking=no \
        ec2-user@${EC2_IP} << 'ENDSSH'
sudo mv /tmp/backend.jar /opt/backend/backend.jar
sudo chown springboot:springboot /opt/backend/backend.jar
sudo systemctl start backend
sudo systemctl enable backend
sleep 5
sudo systemctl status backend --no-pager
ENDSSH
    
    print_info "Application deployed and started!"
    print_info "Check application health: curl ${APP_URL}/actuator/health"
}

# Main execution
main() {
    echo "=========================================="
    echo "AWS CloudFormation Deployment Script"
    echo "Spring Boot Backend Infrastructure"
    echo "=========================================="
    echo
    
    check_prerequisites
    get_inputs
    create_parameters_file
    deploy_stack
    display_outputs
    prompt_jar_deployment
    
    echo
    print_info "Deployment completed successfully!"
    print_info "Check ${STACK_NAME}-outputs.txt for connection details"
}

# Run main function
main
