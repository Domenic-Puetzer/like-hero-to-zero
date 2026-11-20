/**
 * Terraform and provider versions.
 * Major versions are pinned (~>) so builds stay reproducible; the lock file
 * .terraform.lock.hcl pins the exact provider versions.
 */
terraform {
  required_version = ">= 1.5"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }
}

/**
 * Azure provider configuration.
 *
 * prevent_deletion_if_contains_resources = false:
 *   Azure creates some resources on its own (e.g. the Application Insights
 *   "Smart Detection" action group). Terraform does not know them, so by
 *   default it refuses to delete a resource group that still contains them.
 *   This flag disables that check, so destroy removes everything.
 *   Trade-off: foreign resources are no longer protected from deletion.
 */
provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}
