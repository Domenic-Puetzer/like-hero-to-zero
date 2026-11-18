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

/**
 * App Service plan: the compute (size and price) behind the web app.
 *
 * @var app_service_sku  B1 = basic (no autoscale),
 *                       S1 or higher = autoscale possible, P-SKUs = Premium
 * @var zone_redundant   true = spread instances over availability zones
 *
 * zone_balancing_enabled / worker_count: zone redundancy needs a Premium SKU
 * and at least 2 instances, hence worker_count = 2 when the switch is on.
 * The student subscription provides no quota for zone-redundant Premium instances.
 */
resource "azurerm_service_plan" "main" {
  name                   = "asp-${var.project_name}"
  resource_group_name    = azurerm_resource_group.main.name
  location               = azurerm_resource_group.main.location
  zone_balancing_enabled = var.zone_redundant
  worker_count           = var.zone_redundant ? 2 : null
  os_type                = "Linux"
  sku_name               = var.app_service_sku
}

/**
 * Linux web app running the Spring Boot JAR on Java SE 21.
 *
 * The JAR is not part of Terraform; it is deployed separately
 * (az webapp deploy --type jar). A newly created app stays empty until then.
 *
 * https_only: plain HTTP requests are redirected to HTTPS.
 */
resource "azurerm_linux_web_app" "main" {
  name                = "app-${var.project_name}-${random_string.suffix.result}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_service_plan.main.location
  service_plan_id     = azurerm_service_plan.main.id
  https_only          = true

  # System-assigned managed identity: lets the app authenticate to Azure
  # services (e.g. Key Vault) without a password. It is deleted with the app.
  identity {
    type = "SystemAssigned"
  }


  # Environment variables of the app.
  # DB_USERNAME / DB_PASSWORD: Key Vault references. Azure resolves them at
  # runtime with the managed identity, no secret value appears here.
  # DB_NAME: the app reads it via dotenv. There is no .env file in Azure, so
  # the value must be set as an app setting (otherwise startup fails with a
  # NullPointerException).
  #  - SPRING_DATASOURCE_URL: overrides the local JDBC URL, contains no secret,
  #    SSL is enforced (sslMode=REQUIRED).
  #  - APPLICATIONINSIGHTS_*: connection string plus the Java agent extension.
  # The connection string only allows sending telemetry, it is not a password.
  app_settings = {
    DB_USERNAME = "@Microsoft.KeyVault(SecretUri=${azurerm_key_vault_secret.db_username.versionless_id})"
    DB_PASSWORD = "@Microsoft.KeyVault(SecretUri=${azurerm_key_vault_secret.db_password.versionless_id})"
    DB_NAME     = azurerm_mysql_flexible_database.main.name

    SPRING_DATASOURCE_URL = "jdbc:mysql://${azurerm_mysql_flexible_server.main.fqdn}:3306/${azurerm_mysql_flexible_database.main.name}?sslMode=REQUIRED"

    APPLICATIONINSIGHTS_CONNECTION_STRING      = azurerm_application_insights.main.connection_string
    ApplicationInsightsAgent_EXTENSION_VERSION = "~3"
  }

  # HTTP logs written to the app's file system. This does not include the
  # application output (System.out); that is enabled separately with
  # az webapp log config --docker-container-logging filesystem.
  logs {
    http_logs {
      file_system {
        retention_in_mb   = 35
        retention_in_days = 3
      }
    }
  }

  site_config {
    # always_on: true keeps the app loaded even without requests.
    # false lets Azure unload it after idle time, so the next request
    # triggers a slow cold start.
    always_on = false

    # Front Door only (enable_front_door = true): accept requests solely from
    # Front Door (service tag) and only from our own profile (header X-Azure-FDID).
    # All other callers are denied; the allow rule is defined below.
    # Without Front Door the app stays public.
    ip_restriction_default_action = var.enable_front_door ? "Deny" : "Allow"

    dynamic "ip_restriction" {
      for_each = var.enable_front_door ? [1] : []
      content {
        name        = "allow-front-door"
        priority    = 100
        action      = "Allow"
        service_tag = "AzureFrontDoor.Backend"

        headers {
          x_azure_fdid = [azurerm_cdn_frontdoor_profile.main[0].resource_guid]
        }
      }
    }

    application_stack {
      java_server         = "JAVA"
      java_server_version = "21"
      java_version        = "21"
    }
  }
}

/**
 * Looks up who runs Terraform (tenant ID and object ID of the az login).
 * Nothing is created.
 */
data "azurerm_client_config" "current" {}

/**
 * Key Vault: central store for the database credentials.
 *
 * rbac_authorization_enabled: access via Azure roles instead of access policies.
 * soft_delete_retention_days: a deleted vault stays recoverable for 7 days.
 * The name must be globally unique and at most 24 characters long.
 */
resource "azurerm_key_vault" "main" {
  name                       = "kv-${var.project_name}-${random_string.suffix.result}"
  resource_group_name        = azurerm_resource_group.main.name
  location                   = azurerm_resource_group.main.location
  tenant_id                  = data.azurerm_client_config.current.tenant_id
  sku_name                   = "standard"
  rbac_authorization_enabled = true
  soft_delete_retention_days = 7
}

/**
 * Least privilege, part 1: Terraform (the logged-in user) may write secrets.
 * With RBAC, writing secrets requires a role on the vault.
 */
resource "azurerm_role_assignment" "terraform_secrets" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}

/**
 * Secrets: database credentials.
 *
 * The values come from sensitive variables (TF_VAR_...), never from the code.
 * depends_on makes sure the role assignment exists before the secrets are written.
 *
 * Note: Terraform stores the values in plaintext in terraform.tfstate
 * (sensitive = true only hides them in the output), so the state file must
 * not be committed.
 */
resource "azurerm_key_vault_secret" "db_username" {
  name         = "db-username"
  value        = var.db_username
  key_vault_id = azurerm_key_vault.main.id
  depends_on   = [azurerm_role_assignment.terraform_secrets]
}

resource "azurerm_key_vault_secret" "db_password" {
  name         = "db-password"
  value        = var.db_password
  key_vault_id = azurerm_key_vault.main.id
  depends_on   = [azurerm_role_assignment.terraform_secrets]
}

/**
 * Least privilege, part 2: the web app may only read secrets.
 * The role is assigned to the app's managed identity (principal_id).
 */
resource "azurerm_role_assignment" "app_secrets" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_linux_web_app.main.identity[0].principal_id
}

/**
 * Database server: Azure Database for MySQL Flexible Server.
 *
 * @var mysql_sku    server tier, default is the Burstable tier B_Standard_B2ms
 * @var db_username  administrator login (sensitive)
 * @var db_password  administrator password (sensitive)
 *
 * Version 8.4 is used because it is the lates long-term support (LTS) version at the time of writing.
 * Burstable tiers can run with reduced performance once their CPU credits
 * are used up.
 */
resource "azurerm_mysql_flexible_server" "main" {
  name                   = "mysql-${var.project_name}-${random_string.suffix.result}"
  resource_group_name    = azurerm_resource_group.main.name
  location               = azurerm_resource_group.main.location
  administrator_login    = var.db_username
  administrator_password = var.db_password
  sku_name               = var.mysql_sku
  version                = "8.4"

  storage {
    size_gb = 20
  }
}

/**
 * Database inside the MySQL server.
 * Its name is used in the JDBC URL of the web app settings.
 */
resource "azurerm_mysql_flexible_database" "main" {
  name                = "likeherotozero"
  resource_group_name = azurerm_resource_group.main.name
  server_name         = azurerm_mysql_flexible_server.main.name
  charset             = "utf8mb4"
  collation           = "utf8mb4_unicode_ci"
}

/**
 * Firewall rule: 0.0.0.0 - 0.0.0.0 is the special value that allows Azure
 * services to reach the server. Other networks (e.g. a local PC) stay blocked.
 *
 * This is a deliberate simplification: any Azure service can try to connect,
 * but still needs user name, password and SSL. A private endpoint would
 * avoid public access altogether.
 */
resource "azurerm_mysql_flexible_server_firewall_rule" "allow_azure" {
  name                = "allow-azure-services"
  resource_group_name = azurerm_resource_group.main.name
  server_name         = azurerm_mysql_flexible_server.main.name
  start_ip_address    = "0.0.0.0"
  end_ip_address      = "0.0.0.0"
}

/**
 * Log Analytics workspace: central store for logs and metrics.
 * PerGB2018 means pay-as-you-go per ingested GB. Data is kept for 30 days.
 */
resource "azurerm_log_analytics_workspace" "main" {
  name                = "log-${var.project_name}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku                 = "PerGB2018"
  retention_in_days   = 30
}

/**
 * Application Insights: monitors the Java application itself
 * (requests, errors, response times). Its data is stored in the
 * Log Analytics workspace.
 */
resource "azurerm_application_insights" "main" {
  name                = "appi-${var.project_name}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  workspace_id        = azurerm_log_analytics_workspace.main.id
  application_type    = "java"
}

/**
 * Diagnostic settings: send the logs of the web app, Key Vault and MySQL
 * server to the Log Analytics workspace, so all layers are monitored in one place.
 *
 * category_group "allLogs" selects every log category of the service.
 * The Key Vault log also records each access to a secret (audit trail).
 */
resource "azurerm_monitor_diagnostic_setting" "web_app" {
  name                       = "diag-web-app"
  target_resource_id         = azurerm_linux_web_app.main.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id

  enabled_log {
    category_group = "allLogs"
  }
}

resource "azurerm_monitor_diagnostic_setting" "key_vault" {
  name                       = "diag-key-vault"
  target_resource_id         = azurerm_key_vault.main.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id

  enabled_log {
    category_group = "allLogs"
  }
}

resource "azurerm_monitor_diagnostic_setting" "mysql" {
  name                       = "diag-mysql"
  target_resource_id         = azurerm_mysql_flexible_server.main.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id

  enabled_log {
    category_group = "allLogs"
  }
}

/**
 * Action group: defines who is notified when an alert fires.
 * Alert rules and action groups are global resources (no region).
 *
 * @var alert_email  recipient of the alert e-mails
 */
resource "azurerm_monitor_action_group" "main" {
  name                = "ag-${var.project_name}"
  resource_group_name = azurerm_resource_group.main.name
  short_name          = "lhtz-alerts" # max. 12 characters

  email_receiver {
    name          = "admin"
    email_address = var.alert_email
  }
}

/**
 * Metric alert: more than 5 HTTP 5xx errors within 5 minutes.
 * The threshold is an example value. The alert notifies the action group.
 */
resource "azurerm_monitor_metric_alert" "http_5xx" {
  name                = "alert-http-5xx"
  resource_group_name = azurerm_resource_group.main.name
  scopes              = [azurerm_linux_web_app.main.id]
  description         = "More than 5 server errors (HTTP 5xx) within 5 minutes"
  severity            = 2
  frequency           = "PT1M"
  window_size         = "PT5M"

  criteria {
    metric_namespace = "Microsoft.Web/sites"
    metric_name      = "Http5xx"
    aggregation      = "Total"
    operator         = "GreaterThan"
    threshold        = 5
  }

  action {
    action_group_id = azurerm_monitor_action_group.main.id
  }
}

/**
 * Budget: e-mail warning when 80 % of the amount is used.
 *
 * It only warns and does not stop any spending. The amount (10, in the
 * billing currency) is an example value. start_date must be the first day
 * of a month.
 *
 * @var alert_email  recipient of the warning
 */
resource "azurerm_consumption_budget_resource_group" "main" {
  name              = "budget-${var.project_name}"
  resource_group_id = azurerm_resource_group.main.id
  amount            = 10
  time_grain        = "Monthly"

  time_period {
    start_date = "2026-11-01T00:00:00Z"
  }

  notification {
    enabled        = true
    threshold      = 80
    operator       = "GreaterThan"
    threshold_type = "Actual"
    contact_emails = [var.alert_email]
  }
}

/**
 * Autoscale: scales the web app in and out with CPU load.
 *
 * Created only for SKUs above B1 (autoscale needs Standard or higher),
 * hence count = 0 for B1.
 * Rules: add one instance above 70 % average CPU over 5 minutes, remove one
 * below 30 % over 10 minutes, between 1 and 3 instances. The cooldown avoids
 * flapping between scale-out and scale-in. Thresholds are example values.
 * With zone_redundant = true the minimum is 2, matching the instances the
 * plan needs for zone balancing.
 *
 * Do not change these rules in the portal, that causes drift from this code.
 */
resource "azurerm_monitor_autoscale_setting" "main" {
  count               = var.app_service_sku == "B1" ? 0 : 1
  name                = "autoscale-${var.project_name}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  target_resource_id  = azurerm_service_plan.main.id

  profile {
    name = "default"

    capacity {
      default = var.zone_redundant ? 2 : 1
      minimum = var.zone_redundant ? 2 : 1
      maximum = 3
    }

    # Scale out: CPU high
    rule {
      metric_trigger {
        metric_name        = "CpuPercentage"
        metric_resource_id = azurerm_service_plan.main.id
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT5M"
        time_aggregation   = "Average"
        operator           = "GreaterThan"
        threshold          = 70
      }
      scale_action {
        direction = "Increase"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT5M"
      }
    }

    # Scale in: CPU low
    rule {
      metric_trigger {
        metric_name        = "CpuPercentage"
        metric_resource_id = azurerm_service_plan.main.id
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT10M"
        time_aggregation   = "Average"
        operator           = "LessThan"
        threshold          = 30
      }
      scale_action {
        direction = "Decrease"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT10M"
      }
    }
  }
}

/**
 * Optional: Azure Front Door (Standard), only built with enable_front_door.
 *
 * Chain: visitor -> endpoint -> route -> origin group -> origin (the web app).
 * Uses the Standard/Premium resources (azurerm_cdn_frontdoor_*), because
 * Front Door Classic can no longer be created.
 * Not available on Free-Trial and Student subscriptions.
 * When enabled, the web app only accepts traffic from this profile
 * (see ip_restriction in the web app).
 */

/**
 * Front Door profile: the global entry point (CDN and routing).
 * The Standard tier has a base fee per profile, billed hourly.
 */
resource "azurerm_cdn_frontdoor_profile" "main" {
  count               = var.enable_front_door ? 1 : 0
  name                = "afd-${var.project_name}"
  resource_group_name = azurerm_resource_group.main.name
  sku_name            = "Standard_AzureFrontDoor"
}

/**
 * Endpoint: the public hostname visitors use to reach the app via Front Door.
 * The name must be globally unique, hence the random suffix.
 */
resource "azurerm_cdn_frontdoor_endpoint" "main" {
  count                    = var.enable_front_door ? 1 : 0
  name                     = "fde-${var.project_name}-${random_string.suffix.result}"
  cdn_frontdoor_profile_id = azurerm_cdn_frontdoor_profile.main[0].id
}

/**
 * Origin group: the pool of backends behind Front Door.
 *
 * The health probe sends an HTTPS HEAD request to "/" every 100 seconds.
 * An origin that does not answer there is treated as unhealthy.
 * Load balancing uses the default settings.
 */
resource "azurerm_cdn_frontdoor_origin_group" "main" {
  count                    = var.enable_front_door ? 1 : 0
  name                     = "og-${var.project_name}"
  cdn_frontdoor_profile_id = azurerm_cdn_frontdoor_profile.main[0].id

  load_balancing {}

  health_probe {
    path                = "/"
    protocol            = "Https"
    request_type        = "HEAD"
    interval_in_seconds = 100
  }
}

/**
 * Origin: the web app behind Front Door.
 *
 * Requests are forwarded to the app's default hostname over HTTPS (port 443).
 * The same hostname is sent as host header, and the origin certificate name
 * is checked. There is only one origin, so priority and weight have no
 * practical effect.
 */
resource "azurerm_cdn_frontdoor_origin" "main" {
  count                          = var.enable_front_door ? 1 : 0
  name                           = "origin-webapp"
  cdn_frontdoor_origin_group_id  = azurerm_cdn_frontdoor_origin_group.main[0].id
  enabled                        = true
  host_name                      = azurerm_linux_web_app.main.default_hostname
  origin_host_header             = azurerm_linux_web_app.main.default_hostname
  certificate_name_check_enabled = true
  https_port                     = 443
  priority                       = 1
  weight                         = 1000
}

/**
 * Route: connects the endpoint to the origin group.
 *
 * All paths are forwarded to the web app. HTTP requests are redirected to
 * HTTPS, and Front Door talks to the origin over HTTPS only.
 * No caching is configured, so every request is passed through to the app
 * (suitable for dynamic pages).
 */
resource "azurerm_cdn_frontdoor_route" "main" {
  count                         = var.enable_front_door ? 1 : 0
  name                          = "route-webapp"
  cdn_frontdoor_endpoint_id     = azurerm_cdn_frontdoor_endpoint.main[0].id
  cdn_frontdoor_origin_group_id = azurerm_cdn_frontdoor_origin_group.main[0].id
  cdn_frontdoor_origin_ids      = [azurerm_cdn_frontdoor_origin.main[0].id]

  supported_protocols    = ["Http", "Https"]
  patterns_to_match      = ["/*"]
  forwarding_protocol    = "HttpsOnly"
  https_redirect_enabled = true
  link_to_default_domain = true
}
