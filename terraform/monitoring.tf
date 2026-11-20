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
