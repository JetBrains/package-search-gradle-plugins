provider "aws" {
  region = "us-east-2"
}

# GET the default vpc.
resource "aws_default_vpc" "default" {

}

variable "neptune_sg_name" {
  default = "neptune-sg"
}

output "neptune_cluster_instances" {
  value = aws_neptune_cluster.example.cluster_members
}

output "neptune_cluster_endpoint" {
  value = aws_neptune_cluster_instance.example.endpoint
}

## ------ AWS Neptune Cluster  -------------------------------------------------
resource "aws_neptune_cluster" "example" {

  cluster_identifier                  = "neptune-cluster-demo"
  engine                              = "neptune"
  skip_final_snapshot                 = true
  iam_database_authentication_enabled = false
  apply_immediately                   = true
  vpc_security_group_ids              = [aws_security_group.neptune_example.id]

}

resource "aws_neptune_cluster_instance" "example" {
  cluster_identifier = aws_neptune_cluster.example.id
  engine             = "neptune"
  instance_class     = "db.t3.medium"
  apply_immediately  = true
}

resource "aws_security_group" "neptune_example" {
  name        = var.neptune_sg_name
  description = "Allow traffic for ecs"
  vpc_id      = aws_default_vpc.default.id

  ingress {
    from_port   = 8182
    to_port     = 8182
    protocol    = "tcp"
    self        = true
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

}

module "banana" {
  source = modules.org.jetbrains.gradle.project-a
}
