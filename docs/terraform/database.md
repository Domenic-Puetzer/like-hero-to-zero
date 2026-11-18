# Database

File: `database.tf`. Back to the [Terraform README](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/terraform/README.md).

## What it does

It stores the emission data in an Azure Database for MySQL Flexible Server.

## MySQL server (`azurerm_mysql_flexible_server`)

| Setting | Value |
|---|---|
| Name | `mysql-<project_name>-<suffix>` |
| SKU | variable `mysql_sku`, default `B_Standard_B2ms` (Burstable) |
| Version | 8.4 |
| Storage | 20 GB |
| Admin login | variable `db_username` (sensitive) |
| Admin password | variable `db_password` (sensitive) |

- Version 8.4 was chosen because it was the latest long-term support (LTS) version at the time of writing.
- Burstable tiers can run with reduced performance once their CPU credits are used up.
- The credentials come from environment variables and are also written to the Key Vault (see [key-vault.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/key-vault.md)).

## Database (`azurerm_mysql_flexible_database`)

Name `likeherotozero`, character set `utf8mb4`, collation `utf8mb4_unicode_ci`. The name is used in the JDBC URL and in the `DB_NAME` app setting.

## Firewall rule (`azurerm_mysql_flexible_server_firewall_rule`)

`allow-azure-services` with the range `0.0.0.0` to `0.0.0.0`. This special value allows Azure services to reach the server. Other networks (for example a local PC) stay blocked.

This is a deliberate simplification. Any Azure service can try to connect, but still needs user name, password and SSL (the JDBC URL of the app uses `sslMode=REQUIRED`). A private endpoint would avoid public access altogether.

## Logs

The server sends all log categories to Log Analytics (`diag-mysql`, see [monitoring.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/monitoring.md)).
