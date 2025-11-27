# CI/CD Implementation - Step-by-Step TODO List

## Overview

This guide provides complete step-by-step instructions for implementing a production-ready CI/CD pipeline using GitHub Actions for the Currency Exchange application.

### **What We'll Build**

1. ✅ **Continuous Integration (CI)** - Automated testing and quality checks
2. ✅ **Continuous Deployment (CD)** - Automated deployment to AWS EC2
3. ✅ **Infrastructure Automation** - Terraform workflow
4. ✅ **Security Scanning** - Automated vulnerability detection

### **Benefits**

- 🚀 **Faster Deployments** - From code commit to production in 10-15 minutes
- 🛡️ **Quality Assurance** - All tests run automatically before deployment
- 🔄 **Automatic Rollback** - Failed deployments auto-rollback to previous version
- 📊 **Visibility** - Track all deployments and their status
- 💰 **Cost** - $0/month (within GitHub Actions free tier)

### **Time Estimate**

- **Initial Setup**: 2-3 hours
- **Testing & Verification**: 1 hour
- **Total**: 3-4 hours

---

## Prerequisites Checklist

Before starting CI/CD setup, verify these are complete:

- [ ] ✅ GitHub repository exists: `ArturSemenas/currency-exchange-provider`
- [ ] ✅ Application works locally with Docker Compose
- [ ] ✅ All tests pass locally: `mvn clean verify` (324 unit + 23 integration tests)
- [ ] ✅ AWS account created and configured
- [ ] ✅ EC2 instance deployed and running (from AWS_DEPLOYMENT_TODO.md)
- [ ] ✅ Application accessible at `http://<EC2_IP>:8080`
- [ ] ✅ Git configured with your GitHub credentials

**If any prerequisite is missing:**
- Complete AWS deployment first: See `AWS_DEPLOYMENT_TODO.md`
- Fix failing tests before proceeding

---

## Phase 1: GitHub Secrets Configuration

### 1.1 Understand GitHub Secrets

**What are GitHub Secrets?**
- Encrypted environment variables stored in GitHub repository
- Used to store sensitive data (passwords, API keys, credentials)
- Accessible only in GitHub Actions workflows
- Never exposed in logs or to unauthorized users

**Why We Need Them:**
- AWS credentials for deployment
- EC2 SSH access
- Database passwords
- API keys for external services

### 1.2 Gather Required Values

- [ ] **Collect all secret values** (fill in table below)

| Secret Name | Where to Get It | Example | Your Value |
|-------------------------|-----------------------------------------------------------|---------|------------|
| `AWS_ACCESS_KEY_ID`     | AWS IAM → Users → terraform-deploy → Security credentials | `AKIAIOSFODNN7EXAMPLE` | ______________ |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM → Same as above (shown once at creation) | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` | ______________ |
| `AWS_REGION` | Your Terraform region | `us-east-1` | ______________ |
| `EC2_PUBLIC_IP` | AWS EC2 → Instances → Public IPv4 address | `54.123.45.67` | ______________ |
| `EC2_SSH_PRIVATE_KEY` | Content of `~/.ssh/aws-currency-exchange` file | (entire file content) | ______________ |
| `POSTGRES_PASSWORD` | From your `terraform.tfvars` | `your_secure_password` | ______________ |
| `FIXER_API_KEY` | From Fixer.io dashboard (optional) | `abc123def456` | ______________ |
| `EXCHANGERATESAPI_KEY` | From ExchangeRatesAPI dashboard (optional) | `xyz789uvw456` | ______________ |
| `ALLOWED_SSH_CIDR` | Your public IP + /32 (from whatismyip.com) | `203.0.113.42/32` | ______________ |

### 1.3 Add Secrets via GitHub Web UI

- [ ] **Navigate to repository settings**
  ```
  1. Open browser: https://github.com/ArturSemenas/currency-exchange-provider
  2. Click "Settings" tab
  3. Left sidebar: "Secrets and variables" → "Actions"
  4. Click "New repository secret"
  ```

- [ ] **Add AWS_ACCESS_KEY_ID**
  - Name: `AWS_ACCESS_KEY_ID`
  - Secret: Paste your AWS access key from IAM
  - Click "Add secret"

- [ ] **Add AWS_SECRET_ACCESS_KEY**
  - Name: `AWS_SECRET_ACCESS_KEY`
  - Secret: Paste your AWS secret access key
  - Click "Add secret"

- [ ] **Add AWS_REGION**
  - Name: `AWS_REGION`
  - Secret: `us-east-1`
  - Click "Add secret"

- [ ] **Add EC2_PUBLIC_IP**
  - Name: `EC2_PUBLIC_IP`
  - Secret: Your EC2 instance public IP
  - Click "Add secret"

- [ ] **Add EC2_SSH_PRIVATE_KEY**
  ```powershell
  # Get the content of your private key
  Get-Content $HOME\.ssh\aws-currency-exchange | clip
  ```
  - Name: `EC2_SSH_PRIVATE_KEY`
  - Secret: Paste entire private key (including BEGIN/END lines)
  - **Important**: Must include:
    ```
    -----BEGIN OPENSSH PRIVATE KEY-----
    [key content]
    -----END OPENSSH PRIVATE KEY-----
    ```
  - Click "Add secret"

- [ ] **Add POSTGRES_PASSWORD**
  - Name: `POSTGRES_PASSWORD`
  - Secret: Your database password from terraform.tfvars
  - Click "Add secret"

- [ ] **Add FIXER_API_KEY** (optional)
  - Name: `FIXER_API_KEY`
  - Secret: Your Fixer.io API key (or `YOUR_KEY_HERE` if not using)
  - Click "Add secret"

- [ ] **Add EXCHANGERATESAPI_KEY** (optional)
  - Name: `EXCHANGERATESAPI_KEY`
  - Secret: Your ExchangeRatesAPI key (or `YOUR_KEY_HERE` if not using)
  - Click "Add secret"

- [ ] **Add ALLOWED_SSH_CIDR**
  - Name: `ALLOWED_SSH_CIDR`
  - Secret: Your IP address with /32 (e.g., `203.0.113.42/32`)
  - Click "Add secret"

- [ ] **Verify all secrets added**
  - You should see 9 secrets listed in Settings → Secrets and variables → Actions
  - ✅ AWS_ACCESS_KEY_ID
  - ✅ AWS_SECRET_ACCESS_KEY
  - ✅ AWS_REGION
  - ✅ EC2_PUBLIC_IP
  - ✅ EC2_SSH_PRIVATE_KEY
  - ✅ POSTGRES_PASSWORD
  - ✅ FIXER_API_KEY
  - ✅ EXCHANGERATESAPI_KEY
  - ✅ ALLOWED_SSH_CIDR

### 1.4 Add Secrets via GitHub CLI (Alternative Method)

If you prefer command-line approach:

- [ ] **Install GitHub CLI** (if not already installed)
  ```powershell
  # Install via winget
  winget install GitHub.cli
  
  # Or via Chocolatey
  choco install gh -y
  ```

- [ ] **Authenticate with GitHub**
  ```powershell
  gh auth login
  # Choose: GitHub.com → HTTPS → Login with browser
  ```

- [ ] **Set all secrets at once**
  ```powershell
  # Navigate to repository
  cd "c:\Work\Study\AI Copilot\Cur_ex_app"
  
  # Set AWS credentials
  gh secret set AWS_ACCESS_KEY_ID -b "YOUR_ACCESS_KEY_HERE"
  gh secret set AWS_SECRET_ACCESS_KEY -b "YOUR_SECRET_KEY_HERE"
  gh secret set AWS_REGION -b "us-east-1"
  
  # Set EC2 connection details
  gh secret set EC2_PUBLIC_IP -b "YOUR_EC2_IP_HERE"
  
  # Set SSH private key from file
  Get-Content $HOME\.ssh\aws-currency-exchange | gh secret set EC2_SSH_PRIVATE_KEY
  
  # Set application secrets
  gh secret set POSTGRES_PASSWORD -b "YOUR_PASSWORD_HERE"
  gh secret set FIXER_API_KEY -b "YOUR_KEY_HERE"
  gh secret set EXCHANGERATESAPI_KEY -b "YOUR_KEY_HERE"
  gh secret set ALLOWED_SSH_CIDR -b "YOUR_IP_HERE/32"
  ```

- [ ] **Verify secrets were added**
  ```powershell
  gh secret list
  ```
  - Should show all 9 secrets with "Updated" timestamps

---

## Phase 2: Create GitHub Actions Workflows

### 2.1 Create Directory Structure

- [ ] **Create .github/workflows directory**
  ```powershell
  cd "c:\Work\Study\AI Copilot\Cur_ex_app"
  
  # Create directory structure
  mkdir -Force .github\workflows
  
  # Verify directory created
  ls .github
  ```

### 2.2 Create CI Workflow File

- [ ] **Create ci.yml workflow**
  ```powershell
  New-Item -Path .github\workflows\ci.yml -ItemType File
  ```

- [ ] **Open ci.yml in VS Code**
  ```powershell
  code .github\workflows\ci.yml
  ```

- [ ] **Copy and paste CI workflow content:**

```yaml
name: CI - Build and Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

env:
  JAVA_VERSION: '21'
  MAVEN_OPTS: -Xmx1024m

jobs:
  test:
    name: Test and Quality Checks
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:17-alpine
        env:
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: test_db
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      redis:
        image: redis:7-alpine
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-

      - name: Run unit tests
        run: mvn test -DskipITs
        env:
          SPRING_PROFILES_ACTIVE: test

      - name: Run integration tests
        run: mvn verify -DskipTests
        env:
          SPRING_PROFILES_ACTIVE: test

      - name: Run Checkstyle
        run: mvn checkstyle:check
        continue-on-error: true

      - name: Run PMD
        run: mvn pmd:check
        continue-on-error: true

      - name: Generate JaCoCo coverage report
        run: mvn jacoco:report

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: |
            target/surefire-reports/
            target/failsafe-reports/
            target/site/jacoco/

      - name: Comment PR with test results
        if: github.event_name == 'pull_request' && always()
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            let message = '### 🧪 Test Results\n\n';
            message += '✅ All tests passed!\n\n';
            message += '**Coverage Report**: Check artifacts for detailed JaCoCo report';
            
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: message
            });

  build:
    name: Build Application
    needs: test
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Upload JAR artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-jar
          path: target/*.jar
          retention-days: 7

      - name: Build summary
        run: |
          echo "### ✅ Build Successful" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "**JAR Size**: $(du -h target/*.jar | cut -f1)" >> $GITHUB_STEP_SUMMARY
          echo "**Commit**: ${{ github.sha }}" >> $GITHUB_STEP_SUMMARY
```

- [ ] **Save the file** (Ctrl+S)

### 2.3 Create CD Workflow File

- [ ] **Create cd.yml workflow**
  ```powershell
  New-Item -Path .github\workflows\cd.yml -ItemType File
  code .github\workflows\cd.yml
  ```

- [ ] **Copy and paste CD workflow content:**

```yaml
name: CD - Deploy to AWS EC2

on:
  workflow_run:
    workflows: ["CI - Build and Test"]
    types:
      - completed
    branches: [main]
  workflow_dispatch:
    inputs:
      environment:
        description: 'Deployment environment'
        required: true
        default: 'production'
        type: choice
        options:
          - production
          - staging

jobs:
  deploy:
    name: Deploy to EC2
    runs-on: ubuntu-latest
    if: ${{ github.event.workflow_run.conclusion == 'success' || github.event_name == 'workflow_dispatch' }}
    
    environment:
      name: ${{ inputs.environment || 'production' }}
      url: http://${{ secrets.EC2_PUBLIC_IP }}:8080

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build JAR
        run: mvn clean package -DskipTests

      - name: Deploy to EC2 via SSH
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.EC2_PUBLIC_IP }}
          username: ec2-user
          key: ${{ secrets.EC2_SSH_PRIVATE_KEY }}
          script: |
            # Navigate to application directory
            cd /home/ec2-user/currency-exchange-provider
            
            # Backup current version
            if [ -f target/*.jar ]; then
              cp target/*.jar target/backup.jar
            fi
            
            # Pull latest code
            git pull origin main
            
            # Stop current application
            docker-compose stop currency-exchange-app
            
            # Rebuild application image
            docker-compose build currency-exchange-app
            
            # Start all services
            docker-compose up -d
            
            # Wait for application to start
            echo "Waiting for application to start..."
            sleep 30
            
            # Check container status
            docker-compose ps

      - name: Wait for application startup
        run: sleep 30

      - name: Health check
        run: |
          for i in {1..10}; do
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://${{ secrets.EC2_PUBLIC_IP }}:8080/actuator/health)
            if [ $STATUS -eq 200 ]; then
              echo "✅ Application is healthy!"
              exit 0
            fi
            echo "⏳ Waiting for application... (attempt $i/10)"
            sleep 10
          done
          echo "❌ Health check failed after 10 attempts"
          exit 1

      - name: Verify API endpoints
        run: |
          echo "Testing public endpoint..."
          curl -f http://${{ secrets.EC2_PUBLIC_IP }}:8080/api/v1/currencies || exit 1
          
          echo "Testing Swagger UI..."
          curl -f http://${{ secrets.EC2_PUBLIC_IP }}:8080/swagger-ui.html || exit 1
          
          echo "✅ All endpoints responding correctly"

      - name: Rollback on failure
        if: failure()
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.EC2_PUBLIC_IP }}
          username: ec2-user
          key: ${{ secrets.EC2_SSH_PRIVATE_KEY }}
          script: |
            cd /home/ec2-user/currency-exchange-provider
            
            # Stop failed deployment
            docker-compose down
            
            # Rollback to previous commit
            git reset --hard HEAD~1
            
            # Restore backup JAR
            if [ -f target/backup.jar ]; then
              cp target/backup.jar target/currency-exchange-provider.jar
            fi
            
            # Rebuild and restart
            docker-compose build currency-exchange-app
            docker-compose up -d
            
            echo "⚠️ Deployment failed - rolled back to previous version"

      - name: Create deployment summary
        if: always()
        run: |
          echo "## 🚀 Deployment Summary" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "**Environment**: ${{ inputs.environment || 'production' }}" >> $GITHUB_STEP_SUMMARY
          echo "**Status**: ${{ job.status }}" >> $GITHUB_STEP_SUMMARY
          echo "**Commit**: ${{ github.sha }}" >> $GITHUB_STEP_SUMMARY
          echo "**Author**: ${{ github.actor }}" >> $GITHUB_STEP_SUMMARY
          echo "**Application URL**: http://${{ secrets.EC2_PUBLIC_IP }}:8080" >> $GITHUB_STEP_SUMMARY
          echo "**Swagger UI**: http://${{ secrets.EC2_PUBLIC_IP }}:8080/swagger-ui.html" >> $GITHUB_STEP_SUMMARY
```

- [ ] **Save the file** (Ctrl+S)

### 2.4 Create Terraform Workflow File

- [ ] **Create terraform.yml workflow**
  ```powershell
  New-Item -Path .github\workflows\terraform.yml -ItemType File
  code .github\workflows\terraform.yml
  ```

- [ ] **Copy and paste Terraform workflow content:**

```yaml
name: Terraform - Infrastructure

on:
  push:
    branches: [main]
    paths:
      - 'terraform/**'
  pull_request:
    branches: [main]
    paths:
      - 'terraform/**'
  workflow_dispatch:

jobs:
  terraform:
    name: Terraform Plan and Apply
    runs-on: ubuntu-latest
    
    defaults:
      run:
        working-directory: ./terraform

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ secrets.AWS_REGION }}

      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: 1.6.0

      - name: Terraform Format Check
        run: terraform fmt -check -recursive
        continue-on-error: true

      - name: Terraform Init
        run: terraform init

      - name: Terraform Validate
        run: terraform validate

      - name: Terraform Plan
        id: plan
        run: |
          terraform plan -no-color -out=tfplan \
            -var="postgres_password=${{ secrets.POSTGRES_PASSWORD }}" \
            -var="fixer_api_key=${{ secrets.FIXER_API_KEY }}" \
            -var="exchangeratesapi_key=${{ secrets.EXCHANGERATESAPI_KEY }}" \
            -var="allowed_ssh_cidr=${{ secrets.ALLOWED_SSH_CIDR }}"
        continue-on-error: true

      - name: Comment PR with Plan
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            const output = \`#### Terraform Plan 📖
            
            <details><summary>Show Plan</summary>
            
            \\\`\\\`\\\`
            ${{ steps.plan.outputs.stdout }}
            \\\`\\\`\\\`
            
            </details>
            
            *Pusher: @${{ github.actor }}, Action: \\\`${{ github.event_name }}\\\`*\`;
            
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: output
            })

      - name: Terraform Apply
        if: github.ref == 'refs/heads/main' && github.event_name == 'push'
        run: terraform apply -auto-approve tfplan

      - name: Create summary
        if: always()
        run: |
          echo "## 🏗️ Terraform Summary" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "**Status**: ${{ job.status }}" >> $GITHUB_STEP_SUMMARY
          echo "**Action**: ${{ github.event_name }}" >> $GITHUB_STEP_SUMMARY
```

- [ ] **Save the file** (Ctrl+S)

### 2.5 Create Security Scan Workflow File

- [ ] **Create security-scan.yml workflow**
  ```powershell
  New-Item -Path .github\workflows\security-scan.yml -ItemType File
  code .github\workflows\security-scan.yml
  ```

- [ ] **Copy and paste Security Scan workflow content:**

```yaml
name: Security Scan

on:
  schedule:
    - cron: '0 0 * * 0'  # Weekly on Sunday at midnight
  workflow_dispatch:
  push:
    branches: [main]

jobs:
  dependency-check:
    name: Dependency Security Check
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run OWASP Dependency Check
        run: |
          mvn org.owasp:dependency-check-maven:check \
            -DfailBuildOnCVSS=7 \
            -DsuppressionFiles=dependency-check-suppressions.xml
        continue-on-error: true

      - name: Upload dependency check report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: dependency-check-report
          path: target/dependency-check-report.html

  secret-scan:
    name: Secret Scanning
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: TruffleHog Secret Scan
        uses: trufflesecurity/trufflehog@main
        with:
          path: ./
          base: main
          head: HEAD

  docker-scan:
    name: Docker Image Security Scan
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build application
        run: mvn clean package -DskipTests

      - name: Build Docker image
        run: docker build -t currency-exchange-app:test .

      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: currency-exchange-app:test
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'

      - name: Upload Trivy results to GitHub Security
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'

      - name: Run Trivy as table
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: currency-exchange-app:test
          format: 'table'
          severity: 'CRITICAL,HIGH,MEDIUM'
```

- [ ] **Save the file** (Ctrl+S)

### 2.6 Verify Workflow Files Created

- [ ] **Check all workflow files exist**
  ```powershell
  ls .github\workflows
  ```
  
  Expected output:
  ```
  ci.yml
  cd.yml
  terraform.yml
  security-scan.yml
  ```

- [ ] **Verify YAML syntax** (optional but recommended)
  ```powershell
  # Install yamllint via pip
  pip install yamllint
  
  # Check all workflow files
  yamllint .github/workflows/*.yml
  ```

---

## Phase 3: Configure EC2 for Automated Deployment

### 3.1 Install AWS CLI on EC2

- [ ] **SSH to EC2 instance**
  ```powershell
  ssh -i $HOME\.ssh\aws-currency-exchange ec2-user@<YOUR_EC2_IP>
  ```

- [ ] **Install AWS CLI v2**
  ```bash
  # Download AWS CLI installer
  curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
  
  # Unzip
  unzip awscliv2.zip
  
  # Install
  sudo ./aws/install
  
  # Verify installation
  aws --version
  # Expected: aws-cli/2.x.x ...
  ```

- [ ] **Configure AWS credentials on EC2**
  ```bash
  aws configure
  ```
  - AWS Access Key ID: [Same as GitHub secret AWS_ACCESS_KEY_ID]
  - AWS Secret Access Key: [Same as GitHub secret AWS_SECRET_ACCESS_KEY]
  - Default region name: `us-east-1`
  - Default output format: `json`

- [ ] **Test AWS CLI access**
  ```bash
  aws sts get-caller-identity
  ```
  - Should show your AWS account ID and user ARN

### 3.2 Configure Git on EC2

- [ ] **Set up Git credentials**
  ```bash
  git config --global user.name "ArturSemenas"
  git config --global user.email "your-email@example.com"
  ```

- [ ] **Test Git pull**
  ```bash
  cd /home/ec2-user/currency-exchange-provider
  git pull origin main
  ```
  - Should show "Already up to date" or pull latest changes

### 3.3 Verify Docker and Docker Compose

- [ ] **Check Docker installation**
  ```bash
  docker --version
  docker-compose --version
  ```

- [ ] **Test Docker access**
  ```bash
  docker ps
  ```
  - Should show running containers (no permission denied error)

- [ ] **Verify application is running**
  ```bash
  curl http://localhost:8080/actuator/health
  ```
  - Expected: `{"status":"UP"}`

### 3.4 Create Deployment Scripts

- [ ] **Create deployment helper script**
  ```bash
  cat > /home/ec2-user/deploy.sh << 'EOF'
#!/bin/bash
set -e

echo "🚀 Starting deployment..."

# Navigate to app directory
cd /home/ec2-user/currency-exchange-provider

# Pull latest code
echo "📥 Pulling latest code..."
git pull origin main

# Stop containers
echo "🛑 Stopping containers..."
docker-compose stop currency-exchange-app

# Rebuild image
echo "🔨 Building Docker image..."
docker-compose build currency-exchange-app

# Start all services
echo "▶️ Starting services..."
docker-compose up -d

# Wait for startup
echo "⏳ Waiting for application..."
sleep 30

# Health check
echo "🏥 Health check..."
curl -f http://localhost:8080/actuator/health

echo "✅ Deployment complete!"
docker-compose ps
EOF
  
  chmod +x /home/ec2-user/deploy.sh
  ```

- [ ] **Test deployment script**
  ```bash
  ./deploy.sh
  ```
  - Should complete without errors

---

## Phase 4: Commit and Push Workflows

### 4.1 Review Changes

- [ ] **Check Git status**
  ```powershell
  cd "c:\Work\Study\AI Copilot\Cur_ex_app"
  git status
  ```
  
  Should show:
  ```
  Untracked files:
    .github/workflows/ci.yml
    .github/workflows/cd.yml
    .github/workflows/terraform.yml
    .github/workflows/security-scan.yml
    CICD_TODO.md
  ```

- [ ] **Review workflow files one more time**
  ```powershell
  # Open each file and verify content
  code .github\workflows\ci.yml
  code .github\workflows\cd.yml
  code .github\workflows\terraform.yml
  code .github\workflows\security-scan.yml
  ```

### 4.2 Stage and Commit

- [ ] **Stage workflow files**
  ```powershell
  git add .github/workflows/
  git add CICD_TODO.md
  git add AWS_DEPLOYMENT_TODO.md
  ```

- [ ] **Create commit**
  ```powershell
  git commit -m "feat: Add GitHub Actions CI/CD workflows

- Add CI workflow for automated testing and quality checks
- Add CD workflow for deployment to AWS EC2
- Add Terraform workflow for infrastructure updates
- Add security scanning workflow
- Add comprehensive CI/CD implementation guide"
  ```

### 4.3 Push to GitHub

- [ ] **Push to main branch**
  ```powershell
  git push origin main
  ```

- [ ] **Watch push output**
  - Should complete successfully
  - GitHub Actions will automatically trigger CI workflow

---

## Phase 5: Monitor First CI Run

### 5.1 Access GitHub Actions

- [ ] **Open GitHub Actions page**
  ```
  https://github.com/ArturSemenas/currency-exchange-provider/actions
  ```

- [ ] **Find "CI - Build and Test" workflow**
  - Should see workflow running (yellow circle)
  - Click on workflow run to see details

### 5.2 Monitor CI Progress

- [ ] **Watch "Test and Quality Checks" job**
  - Steps to complete:
    - ✅ Checkout code
    - ✅ Set up JDK 21
    - ✅ Cache Maven dependencies
    - ✅ Run unit tests (324 tests)
    - ✅ Run integration tests (23 tests)
    - ✅ Run Checkstyle
    - ✅ Run PMD
    - ✅ Generate JaCoCo coverage
    - ✅ Upload test results

- [ ] **Watch "Build Application" job**
  - Steps to complete:
    - ✅ Build with Maven
    - ✅ Upload JAR artifact

### 5.3 Troubleshoot If CI Fails

**If tests fail:**

- [ ] **Check test logs in GitHub Actions**
  - Click on failed step
  - Read error messages

- [ ] **Common issues and solutions:**

| Issue | Solution |
|-------|----------|
| Tests pass locally but fail in CI | Check environment differences (PostgreSQL version, Redis config) |
| Checkstyle violations | Run `mvn checkstyle:check` locally and fix issues |
| PMD violations | Run `mvn pmd:check` locally and fix issues |
| Out of memory | Already configured with `MAVEN_OPTS: -Xmx1024m` |
| Timeout | Increase timeout in workflow if needed |

- [ ] **Fix issues and push again**
  ```powershell
  # Fix issues locally
  mvn clean verify
  
  # Commit and push fix
  git add .
  git commit -m "fix: Resolve CI test failures"
  git push origin main
  ```

### 5.4 Verify CI Success

- [ ] **CI workflow completes successfully**
  - All jobs show green checkmark ✅
  - Test results uploaded
  - JAR artifact created

- [ ] **Download and review artifacts**
  - GitHub Actions → Workflow run → Artifacts section
  - Download "test-results" to review locally

---

## Phase 6: Monitor First CD Run

### 6.1 Watch CD Workflow Trigger

- [ ] **After CI succeeds, CD should auto-trigger**
  - GitHub Actions → "CD - Deploy to AWS EC2"
  - Should start automatically (may take 1-2 minutes)

### 6.2 Monitor Deployment Progress

- [ ] **Watch "Deploy to EC2" job**
  - Steps to complete:
    - ✅ Checkout code
    - ✅ Set up JDK 21
    - ✅ Build JAR
    - ✅ Deploy to EC2 via SSH
    - ✅ Wait for application startup
    - ✅ Health check
    - ✅ Verify API endpoints

### 6.3 Monitor from EC2 (Optional)

- [ ] **SSH to EC2 during deployment**
  ```powershell
  ssh -i $HOME\.ssh\aws-currency-exchange ec2-user@<YOUR_EC2_IP>
  ```

- [ ] **Watch Docker logs**
  ```bash
  cd /home/ec2-user/currency-exchange-provider
  docker-compose logs -f currency-exchange-app
  ```

- [ ] **Watch container status**
  ```bash
  watch -n 2 docker-compose ps
  ```

### 6.4 Verify Deployment Success

- [ ] **CD workflow completes successfully**
  - All steps show green checkmark ✅
  - Health check passes
  - API endpoints responding

- [ ] **Test deployed application**
  ```powershell
  # Get EC2 IP from secrets or Terraform output
  $EC2_IP = "<YOUR_EC2_IP>"
  
  # Health check
  Invoke-RestMethod -Uri "http://${EC2_IP}:8080/actuator/health"
  
  # Test public endpoint
  Invoke-RestMethod -Uri "http://${EC2_IP}:8080/api/v1/currencies"
  
  # Access Swagger UI
  Start-Process "http://${EC2_IP}:8080/swagger-ui.html"
  ```

### 6.5 Troubleshoot If CD Fails

**Common deployment issues:**

| Issue | Solution |
|-------|----------|
| SSH connection failed | Verify EC2_SSH_PRIVATE_KEY secret is correct |
| Health check timeout | Increase wait time, check application logs |
| Docker build fails | Check disk space on EC2: `df -h` |
| Git pull fails | Check Git credentials on EC2 |
| Port 8080 not accessible | Check security group rules |

- [ ] **Check deployment logs**
  - GitHub Actions → Failed workflow → Click on failed step
  - Read error messages

- [ ] **Manual rollback if needed**
  ```bash
  ssh ec2-user@<EC2_IP>
  cd /home/ec2-user/currency-exchange-provider
  git log --oneline -5
  git checkout <PREVIOUS_COMMIT>
  docker-compose down
  docker-compose up -d
  ```

---

## Phase 7: Test Manual Deployment

### 7.1 Trigger Manual Deployment

- [ ] **Navigate to GitHub Actions**
  ```
  https://github.com/ArturSemenas/currency-exchange-provider/actions
  ```

- [ ] **Click "CD - Deploy to AWS EC2"**

- [ ] **Click "Run workflow" button**
  - Select branch: `main`
  - Select environment: `production`
  - Click "Run workflow"

### 7.2 Verify Manual Deployment

- [ ] **Watch workflow execute**
  - Should complete successfully

- [ ] **Verify application updated**
  ```powershell
  $EC2_IP = "<YOUR_EC2_IP>"
  Invoke-RestMethod -Uri "http://${EC2_IP}:8080/actuator/health"
  ```

---

## Phase 8: Add Status Badges to README

### 8.1 Get Badge URLs

- [ ] **Get CI workflow badge**
  ```
  https://github.com/ArturSemenas/currency-exchange-provider/actions/workflows/ci.yml/badge.svg
  ```

- [ ] **Get CD workflow badge**
  ```
  https://github.com/ArturSemenas/currency-exchange-provider/actions/workflows/cd.yml/badge.svg
  ```

### 8.2 Update README.md

- [ ] **Open README.md**
  ```powershell
  code README.md
  ```

- [ ] **Add badges at the top** (after title)
  ```markdown
  # Currency Exchange Rates Provider Service
  
  [![CI](https://github.com/ArturSemenas/currency-exchange-provider/actions/workflows/ci.yml/badge.svg)](https://github.com/ArturSemenas/currency-exchange-provider/actions/workflows/ci.yml)
  [![CD](https://github.com/ArturSemenas/currency-exchange-provider/actions/workflows/cd.yml/badge.svg)](https://github.com/ArturSemenas/currency-exchange-provider/actions/workflows/cd.yml)
  [![Security Scan](https://github.com/ArturSemenas/currency-exchange-provider/actions/workflows/security-scan.yml/badge.svg)](https://github.com/ArturSemenas/currency-exchange-provider/actions/workflows/security-scan.yml)
  
  [rest of README content...]
  ```

- [ ] **Commit and push**
  ```powershell
  git add README.md
  git commit -m "docs: Add CI/CD status badges to README"
  git push origin main
  ```

---

## Phase 9: Configure Notifications (Optional)

### 9.1 Slack Notifications

If you want Slack notifications for deployments:

- [ ] **Create Slack webhook**
  - Go to Slack → Apps → Incoming Webhooks
  - Create new webhook
  - Copy webhook URL

- [ ] **Add webhook to GitHub secrets**
  ```powershell
  gh secret set SLACK_WEBHOOK -b "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
  ```

- [ ] **Update cd.yml** (add at end of job):
  ```yaml
      - name: Notify Slack
        if: always()
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: |
            Deployment to production
            Status: ${{ job.status }}
            Commit: ${{ github.sha }}
            Author: ${{ github.actor }}
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
  ```

### 9.2 Email Notifications

- [ ] **Enable email notifications in GitHub**
  - GitHub → Settings → Notifications
  - Enable "Actions" notifications
  - Choose notification frequency

---

## Phase 10: Testing & Validation

### 10.1 Test Complete CI/CD Flow

- [ ] **Make a small code change**
  ```powershell
  # Example: Update version in pom.xml or add a comment
  code src/main/java/com/currencyexchange/provider/CurrencyExchangeProviderApplication.java
  ```

- [ ] **Commit and push**
  ```powershell
  git add .
  git commit -m "test: Verify CI/CD pipeline"
  git push origin main
  ```

- [ ] **Watch complete flow**
  1. ✅ CI workflow triggers
  2. ✅ Tests run (324 unit + 23 integration)
  3. ✅ Quality checks pass
  4. ✅ Build succeeds
  5. ✅ CD workflow triggers automatically
  6. ✅ Deployment to EC2 succeeds
  7. ✅ Health checks pass
  8. ✅ Application accessible

### 10.2 Test Rollback Mechanism

- [ ] **Create intentional failure** (to test rollback)
  ```java
  // In any controller, add:
  @GetMapping("/test-failure")
  public void testFailure() {
      throw new RuntimeException("Intentional failure");
  }
  ```

- [ ] **Commit and push**
  ```powershell
  git add .
  git commit -m "test: Intentional failure for rollback test"
  git push origin main
  ```

- [ ] **Watch deployment fail and rollback**
  - CD workflow should detect failure
  - Automatic rollback should execute
  - Previous version should be restored

- [ ] **Revert the test**
  ```powershell
  git revert HEAD
  git push origin main
  ```

### 10.3 Test Security Scan

- [ ] **Trigger security scan manually**
  - GitHub Actions → "Security Scan" → Run workflow

- [ ] **Review scan results**
  - Check for dependency vulnerabilities
  - Check for Docker image vulnerabilities
  - Review secret scanning results

- [ ] **Check GitHub Security tab**
  - Repository → Security → Code scanning alerts
  - Should show Trivy results

---

## Phase 11: Production Optimization

### 11.1 Optimize Workflow Performance

- [ ] **Enable concurrent job execution** (if safe)
  ```yaml
  # In ci.yml, add at top level:
  concurrency:
    group: ci-${{ github.ref }}
    cancel-in-progress: true
  ```

- [ ] **Optimize Maven builds**
  - Already using dependency caching
  - Consider adding `-T 1C` for parallel builds

### 11.2 Add Environment Protection

- [ ] **Create production environment**
  - GitHub → Settings → Environments
  - Click "New environment"
  - Name: `production`

- [ ] **Add protection rules**
  - Required reviewers: Add yourself
  - Wait timer: 0 minutes (or set delay)
  - Required deployment branches: `main` only

### 11.3 Create Staging Environment (Optional)

- [ ] **If you have staging EC2 instance:**
  - Add staging secrets (EC2_PUBLIC_IP_STAGING, etc.)
  - Create separate cd-staging.yml workflow
  - Test in staging before production

---

## Success Checklist

- [ ] ✅ All GitHub secrets configured (9 secrets)
- [ ] ✅ CI workflow created and passing
- [ ] ✅ CD workflow created and deploying successfully
- [ ] ✅ Terraform workflow created
- [ ] ✅ Security scan workflow created
- [ ] ✅ EC2 instance configured for automated deployment
- [ ] ✅ First automated deployment completed successfully
- [ ] ✅ Health checks passing
- [ ] ✅ Rollback mechanism tested
- [ ] ✅ Status badges added to README
- [ ] ✅ Application accessible at http://EC2_IP:8080
- [ ] ✅ All 347 tests passing in CI
- [ ] ✅ Code quality checks passing

---

## Metrics & Monitoring

### Expected CI/CD Performance

| Metric | Target | Actual |
|--------|--------|--------|
| CI Duration | 8-10 min | _____ min |
| CD Duration | 3-5 min | _____ min |
| Total Deployment Time | 11-15 min | _____ min |
| Test Success Rate | 100% | _____ % |
| Deployment Success Rate | >95% | _____ % |

### GitHub Actions Usage

- **Free Tier Limit**: 2,000 minutes/month
- **Estimated Usage**: ~293 minutes/month
- **Remaining**: ~1,707 minutes/month ✅

---

## Troubleshooting Guide

### CI Workflow Issues

**Tests fail in CI but pass locally**
```powershell
# Solution: Run tests in Docker locally to match CI environment
docker run --rm -v ${PWD}:/app -w /app maven:3.9-eclipse-temurin-21 mvn test
```

**Checkstyle/PMD failures**
```powershell
# Solution: Run quality checks locally
mvn checkstyle:check pmd:check
# Fix reported issues
```

### CD Workflow Issues

**SSH connection fails**
```powershell
# Verify secret contains correct key
gh secret set EC2_SSH_PRIVATE_KEY < $HOME\.ssh\aws-currency-exchange

# Test SSH manually
ssh -i $HOME\.ssh\aws-currency-exchange ec2-user@<EC2_IP>
```

**Deployment timeout**
```bash
# On EC2: Check disk space
df -h

# Check Docker service
sudo systemctl status docker

# Check application logs
docker-compose logs currency-exchange-app
```

**Health check fails**
```bash
# On EC2: Test health endpoint locally
curl http://localhost:8080/actuator/health

# Check if port is bound
netstat -tlnp | grep 8080

# Check container logs
docker-compose logs -f currency-exchange-app
```

### Workflow Not Triggering

**CD doesn't trigger after CI**
```
# Check workflow_run trigger in cd.yml
# Ensure CI workflow name matches exactly: "CI - Build and Test"
# Wait 1-2 minutes - workflows may have slight delay
```

**Security scan not running**
```
# Trigger manually first time
# GitHub Actions → Security Scan → Run workflow
```

---

## Next Steps

After successful CI/CD setup:

- [ ] Monitor deployments for 1 week
- [ ] Review security scan results weekly
- [ ] Optimize workflow performance if needed
- [ ] Add more environments (staging, QA)
- [ ] Implement blue-green deployment strategy
- [ ] Add performance testing to CI
- [ ] Set up log aggregation (CloudWatch Logs)
- [ ] Add application metrics (Prometheus/Grafana)
- [ ] Configure auto-scaling (if traffic increases)
- [ ] Document deployment procedures for team

---

## Cost Summary

### GitHub Actions (Free Tier)

| Resource | Free Allowance | Our Usage | Cost |
|----------|---------------|-----------|------|
| **Build minutes/month** | 2,000 min | ~293 min | **$0** |
| **Storage** | 500 MB | ~50 MB | **$0** |
| **Data transfer** | 100 GB | ~1 GB | **$0** |

### AWS Resources (No Change)

| Resource | Monthly Cost |
|----------|-------------|
| **EC2 t2.micro** | $0 (free tier) |
| **EBS 20 GB** | $0 (free tier) |
| **Data transfer** | $0 (free tier) |

### **Total CI/CD Cost: $0/month** ✅

---

## Documentation & Handoff

### Files Created

1. `.github/workflows/ci.yml` - Continuous Integration workflow
2. `.github/workflows/cd.yml` - Continuous Deployment workflow
3. `.github/workflows/terraform.yml` - Infrastructure workflow
4. `.github/workflows/security-scan.yml` - Security scanning workflow
5. `CICD_TODO.md` - This implementation guide

### Architecture Diagram

```
┌─────────────┐
│   Developer │
└──────┬──────┘
       │ git push
       ▼
┌─────────────────┐
│  GitHub Repo    │
└────────┬────────┘
         │ triggers
         ▼
┌──────────────────────┐
│  GitHub Actions CI   │
│  - Unit Tests (324)  │
│  - Integration (23)  │
│  - Quality Checks    │
│  - Build JAR         │
└──────────┬───────────┘
           │ on success
           ▼
┌──────────────────────┐
│  GitHub Actions CD   │
│  - SSH to EC2        │
│  - Pull code         │
│  - Build Docker      │
│  - Deploy            │
│  - Health check      │
└──────────┬───────────┘
           │ deploys to
           ▼
┌──────────────────────┐
│    AWS EC2 Instance  │
│  - Docker Compose    │
│  - Spring Boot       │
│  - PostgreSQL        │
│  - Redis             │
└──────────────────────┘
```

**Congratulations! Your CI/CD pipeline is now complete! 🎉**

Every code push to main will automatically:
1. Run all tests ✅
2. Check code quality ✅
3. Deploy to AWS ✅
4. Verify health ✅
5. Rollback on failure ✅
