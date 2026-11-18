/**
 * like-hero-to-zero on Azure (Terraform)
 *
 * Hosts the Spring Boot application (Java 21, MySQL) on Azure.
 *
 * Architecture:
 *   Visitor -> Linux web app (Spring Boot JAR) -> MySQL Flexible Server
 *   Secrets:    Key Vault, read by the app through a managed identity
 *   Monitoring: Log Analytics, Application Insights, alert, budget
 *   Scaling:    autoscale rules on the App Service plan (above B1)
 *
 * Optional switches (see variables.tf):
 *   enable_front_door - Front Door Standard as global entry point; the web app
 *                       then accepts only Front Door traffic. Needs a regular
 *                       (non Free-Trial/Student) subscription.
 *   zone_redundant    - spreads the plan over availability zones. Needs a
 *                       Premium SKU, 2+ instances and quota for zone-redundant
 *                       instances.
 *
 * The application JAR is not part of Terraform. It is deployed separately
 * with `az webapp deploy`.
 *
 * Required environment variables:
 *   TF_VAR_db_username, TF_VAR_db_password, TF_VAR_alert_email
 */

/**
 * Resource group: the container for all resources below.
 * Deleting it (terraform destroy) removes everything inside.
 *
 * @var location      Azure region of all resources
 * @var project_name  prefix for all resource names
 */
resource "azurerm_resource_group" "main" {
  name     = "rg-${var.project_name}"
  location = var.location

  tags = {
    project = var.project_name
  }
}

/**
 * Random suffix for globally unique names (web app, Key Vault, MySQL,
 * Front Door endpoint). It is generated once per build, so a fresh build
 * gets a new suffix.
 */
resource "random_string" "suffix" {
  length  = 5
  special = false
  upper   = false
}
