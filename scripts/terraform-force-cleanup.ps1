#!/usr/bin/env pwsh
# Force cleanup of AWS resources and sync Terraform state
# Use this ONLY when Terraform state is out of sync with actual AWS resources

param(
    [switch]$DryRun = $false
)

$ErrorActionPreference = "Stop"
$region = "eu-north-1"

Write-Host "=== Terraform State Cleanup Tool ===" -ForegroundColor Cyan
Write-Host ""

if ($DryRun) {
    Write-Host "DRY RUN MODE - No changes will be made" -ForegroundColor Yellow
    Write-Host ""
}

# Check if we're in the terraform directory
if (-not (Test-Path "main.tf")) {
    Write-Host "Error: Must run from terraform directory" -ForegroundColor Red
    exit 1
}

# 1. Download current state from S3
Write-Host "1. Downloading current Terraform state from S3..." -ForegroundColor Cyan
$stateBackup = "terraform.tfstate.backup-$(Get-Date -Format 'yyyy-MM-dd-HHmmss')"
try {
    aws s3 cp s3://currency-exchange-terraform-state/terraform.tfstate $stateBackup --region $region
    Write-Host "   ✓ State backed up to: $stateBackup" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Failed to download state (bucket might not exist)" -ForegroundColor Yellow
}

# 2. List resources Terraform thinks exist
Write-Host ""
Write-Host "2. Resources in Terraform state:" -ForegroundColor Cyan
if (Test-Path $stateBackup) {
    $state = Get-Content $stateBackup | ConvertFrom-Json
    $resources = $state.resources
    if ($resources.Count -gt 0) {
        foreach ($resource in $resources) {
            Write-Host "   - $($resource.type).$($resource.name)" -ForegroundColor White
        }
    } else {
        Write-Host "   (empty - no resources tracked)" -ForegroundColor Yellow
    }
}

# 3. List actual AWS resources
Write-Host ""
Write-Host "3. Actual AWS resources with Application tag:" -ForegroundColor Cyan

# EC2 Instances
Write-Host "   Checking EC2 instances..." -ForegroundColor Gray
$instances = aws ec2 describe-instances `
    --region $region `
    --filters "Name=tag:Application,Values=currency-exchange-app" "Name=instance-state-name,Values=pending,running,stopping,stopped" `
    --query 'Reservations[*].Instances[*].[InstanceId,State.Name,PublicIpAddress]' `
    --output json | ConvertFrom-Json

if ($instances.Count -gt 0) {
    foreach ($instance in $instances) {
        Write-Host "   - EC2 Instance: $($instance[0]) ($($instance[1])) IP: $($instance[2])" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ✓ No instances" -ForegroundColor Green
}

# Key Pairs
Write-Host "   Checking Key Pairs..." -ForegroundColor Gray
$keys = aws ec2 describe-key-pairs `
    --region $region `
    --query 'KeyPairs[?KeyName==`currency-exchange-app-key`].[KeyName,KeyPairId]' `
    --output json | ConvertFrom-Json

if ($keys.Count -gt 0) {
    foreach ($key in $keys) {
        Write-Host "   - Key Pair: $($key[0]) ($($key[1]))" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ✓ No key pairs" -ForegroundColor Green
}

# VPCs
Write-Host "   Checking VPCs..." -ForegroundColor Gray
$vpcs = aws ec2 describe-vpcs `
    --region $region `
    --filters "Name=tag:Application,Values=currency-exchange-app" `
    --query 'Vpcs[*].[VpcId,CidrBlock]' `
    --output json | ConvertFrom-Json

if ($vpcs.Count -gt 0) {
    foreach ($vpc in $vpcs) {
        Write-Host "   - VPC: $($vpc[0]) ($($vpc[1]))" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ✓ No VPCs" -ForegroundColor Green
}

# Security Groups (non-default)
Write-Host "   Checking Security Groups..." -ForegroundColor Gray
$sgs = aws ec2 describe-security-groups `
    --region $region `
    --filters "Name=tag:Application,Values=currency-exchange-app" `
    --query 'SecurityGroups[*].[GroupId,GroupName]' `
    --output json | ConvertFrom-Json

if ($sgs.Count -gt 0) {
    foreach ($sg in $sgs) {
        Write-Host "   - Security Group: $($sg[0]) ($($sg[1]))" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ✓ No security groups" -ForegroundColor Green
}

# 4. Offer cleanup options
Write-Host ""
Write-Host "4. Cleanup Options:" -ForegroundColor Cyan
Write-Host ""
Write-Host "   A. Refresh Terraform state only (sync with AWS)" -ForegroundColor White
Write-Host "   B. Run Terraform destroy (will use refreshed state)" -ForegroundColor White
Write-Host "   C. Force remove all AWS resources via CLI (DANGEROUS)" -ForegroundColor Red
Write-Host "   Q. Quit without changes" -ForegroundColor Gray
Write-Host ""

if (-not $DryRun) {
    $choice = Read-Host "Select option [A/B/C/Q]"
    
    switch ($choice.ToUpper()) {
        "A" {
            Write-Host ""
            Write-Host "Initializing Terraform..." -ForegroundColor Cyan
            terraform init -reconfigure
            
            Write-Host "Refreshing state..." -ForegroundColor Cyan
            terraform apply -refresh-only -auto-approve
            
            Write-Host ""
            Write-Host "✓ State refreshed. Run 'terraform plan' to see what needs cleanup" -ForegroundColor Green
        }
        "B" {
            Write-Host ""
            Write-Host "Initializing Terraform..." -ForegroundColor Cyan
            terraform init -reconfigure
            
            Write-Host "Refreshing state..." -ForegroundColor Cyan
            terraform apply -refresh-only -auto-approve
            
            Write-Host "Running destroy..." -ForegroundColor Cyan
            terraform destroy -auto-approve
            
            Write-Host ""
            Write-Host "✓ Terraform destroy completed" -ForegroundColor Green
        }
        "C" {
            Write-Host ""
            Write-Host "WARNING: This will forcefully remove resources without updating Terraform state!" -ForegroundColor Red
            $confirm = Read-Host "Type 'DELETE' to confirm"
            
            if ($confirm -eq "DELETE") {
                Write-Host "Terminating instances..." -ForegroundColor Yellow
                foreach ($instance in $instances) {
                    aws ec2 terminate-instances --region $region --instance-ids $instance[0]
                }
                
                Write-Host "Deleting key pairs..." -ForegroundColor Yellow
                foreach ($key in $keys) {
                    aws ec2 delete-key-pair --region $region --key-name $key[0]
                }
                
                Start-Sleep -Seconds 10
                
                Write-Host "Deleting security groups..." -ForegroundColor Yellow
                foreach ($sg in $sgs) {
                    aws ec2 delete-security-group --region $region --group-id $sg[0]
                }
                
                Write-Host "Deleting VPCs..." -ForegroundColor Yellow
                foreach ($vpc in $vpcs) {
                    # Get and delete subnets
                    $subnets = aws ec2 describe-subnets --region $region --filters "Name=vpc-id,Values=$($vpc[0])" --query 'Subnets[*].SubnetId' --output json | ConvertFrom-Json
                    foreach ($subnet in $subnets) {
                        aws ec2 delete-subnet --region $region --subnet-id $subnet
                    }
                    
                    # Get and delete internet gateways
                    $igws = aws ec2 describe-internet-gateways --region $region --filters "Name=attachment.vpc-id,Values=$($vpc[0])" --query 'InternetGateways[*].InternetGatewayId' --output json | ConvertFrom-Json
                    foreach ($igw in $igws) {
                        aws ec2 detach-internet-gateway --region $region --internet-gateway-id $igw --vpc-id $vpc[0]
                        aws ec2 delete-internet-gateway --region $region --internet-gateway-id $igw
                    }
                    
                    # Get and delete route tables (non-main)
                    $rtbs = aws ec2 describe-route-tables --region $region --filters "Name=vpc-id,Values=$($vpc[0])" "Name=association.main,Values=false" --query 'RouteTables[*].RouteTableId' --output json | ConvertFrom-Json
                    foreach ($rtb in $rtbs) {
                        aws ec2 delete-route-table --region $region --route-table-id $rtb
                    }
                    
                    aws ec2 delete-vpc --region $region --vpc-id $vpc[0]
                }
                
                Write-Host ""
                Write-Host "✓ AWS resources deleted via CLI" -ForegroundColor Green
                Write-Host ""
                Write-Host "WARNING: Terraform state is now OUT OF SYNC!" -ForegroundColor Red
                Write-Host "Run option A or B to fix the state" -ForegroundColor Yellow
            } else {
                Write-Host "Cancelled" -ForegroundColor Gray
            }
        }
        default {
            Write-Host "Cancelled" -ForegroundColor Gray
        }
    }
} else {
    Write-Host "DRY RUN - No actions taken" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Done!" -ForegroundColor Cyan
