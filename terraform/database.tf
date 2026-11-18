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
