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
