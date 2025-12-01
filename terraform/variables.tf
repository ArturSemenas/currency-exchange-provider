# Input variables for AWS Terraform deployment

variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "EC2 instance type (t2.micro for free tier)"
  type        = string
  default     = "t2.micro"
}

variable "ssh_public_key_path" {
  description = "Path to SSH public key file"
  type        = string
  default     = "~/.ssh/aws-currency-exchange.pub"
}

variable "allowed_ssh_cidr" {
  description = "CIDR block allowed for SSH access (your IP/32)"
  type        = string
}

variable "app_name" {
  description = "Application name for tagging"
  type        = string
  default     = "currency-exchange-app"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "production"
}

variable "postgres_password" {
  description = "PostgreSQL database password"
  type        = string
  sensitive   = true
}

variable "fixer_api_key" {
  description = "Fixer.io API key (optional; use 'mock' for mock providers)"
  type        = string
  sensitive   = true
  default     = "mock"
}

variable "exchangeratesapi_key" {
  description = "ExchangeRatesAPI key (optional; use 'mock' for mock providers)"
  type        = string
  sensitive   = true
  default     = "mock"
}

variable "root_volume_size" {
  description = "Root EBS volume size in GB (max 30 for free tier)"
  type        = number
  default     = 30
}

variable "github_deploy_key" {
  description = "GitHub deploy key private key content for repository access"
  type        = string
  sensitive   = true
}
