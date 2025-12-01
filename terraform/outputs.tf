# Outputs for Terraform deployment
# Note: IAM user outputs removed - using existing terraform-deploy user

output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.app.id
}

output "public_ip" {
  description = "Public IPv4 address (auto-assigned)"
  value       = aws_instance.app.public_ip
}

output "ipv6_address" {
  description = "Public IPv6 address (free)"
  value       = aws_instance.app.ipv6_addresses[0]
}

output "public_dns" {
  description = "Public DNS name"
  value       = aws_instance.app.public_dns
}

output "ssh_command" {
  description = "SSH command to connect to instance"
  value       = "ssh -i ~/.ssh/aws-currency-exchange ec2-user@${aws_instance.app.public_ip}"
}

output "ssh_command_ipv6" {
  description = "SSH command using IPv6"
  value       = "ssh -i ~/.ssh/aws-currency-exchange ec2-user@${aws_instance.app.ipv6_addresses[0]}"
}

output "app_url" {
  description = "Application URL (IPv4)"
  value       = "http://${aws_instance.app.public_ip}:8080"
}

output "app_url_ipv6" {
  description = "Application URL (IPv6 - free)"
  value       = "http://[${aws_instance.app.ipv6_addresses[0]}]:8080"
}

output "swagger_url" {
  description = "Swagger UI URL (IPv4)"
  value       = "http://${aws_instance.app.public_ip}:8080/swagger-ui.html"
}

output "swagger_url_ipv6" {
  description = "Swagger UI URL (IPv6 - free)"
  value       = "http://[${aws_instance.app.ipv6_addresses[0]}]:8080/swagger-ui.html"
}

output "security_group_id" {
  description = "Security group ID"
  value       = aws_security_group.app.id
}

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}
