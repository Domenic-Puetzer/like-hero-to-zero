# App Service

File: `app_service.tf`. Back to the [Terraform README](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/terraform/README.md).

## What it does

It provides the compute for the application: an App Service plan (size and price), a Linux web app on it, and an autoscale setting that adds or removes instances.

## App Service plan (`azurerm_service_plan`)

| Setting | Value |
|---|---|
| Name | `asp-<project_name>` |
| OS | Linux |
| SKU | variable `app_service_sku`, default `S1` |
| Zone balancing | variable `zone_redundant` |
| Worker count | 2 if `zone_redundant = true`, otherwise not set |

- `B1` has no autoscale. `S1` or higher can scale. `P`-SKUs are Premium.
- Zone redundancy needs a Premium SKU and at least 2 instances. See [front-door.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/front-door.md).

## Web app (`azurerm_linux_web_app`)

| Setting | Value |
|---|---|
| Name | `app-<project_name>-<suffix>` |
| Runtime | Java SE 21 |
| `https_only` | `true` (HTTP is redirected to HTTPS) |
| Identity | System-assigned Managed Identity |
| `always_on` | `false` |

`always_on = false` lets Azure unload the app after idle time. The next request then triggers a slow cold start.

The JAR file is **not** part of Terraform. It is deployed with `az webapp deploy`. A new web app stays empty until then.

### App settings

| Setting | Content |
|---|---|
| `DB_USERNAME` | Key Vault reference to the secret `db-username` |
| `DB_PASSWORD` | Key Vault reference to the secret `db-password` |
| `DB_NAME` | Name of the database |
| `SPRING_DATASOURCE_URL` | JDBC URL to the MySQL server with `sslMode=REQUIRED` |
| `APPLICATIONINSIGHTS_CONNECTION_STRING` | Telemetry connection string (not a password, only allows sending telemetry) |
| `ApplicationInsightsAgent_EXTENSION_VERSION` | `~3` (Java agent) |

### Logs

HTTP logs are written to the file system of the app (3 days, up to 35 MB). Application output (`System.out`) is not included. Enable it with:

```
az webapp log config --name <APP-NAME> --resource-group rg-likeherotozero --docker-container-logging filesystem
```

### Front Door restriction

With `enable_front_door = true`, the default action for access is `Deny`. One allow rule lets in only the service tag `AzureFrontDoor.Backend` with the matching profile ID in the header `X-Azure-FDID`. Without Front Door, the app stays public.

## Autoscale (`azurerm_monitor_autoscale_setting`)

Created only if `app_service_sku` is not `B1`.

| Rule | Condition | Action | Cooldown |
|---|---|---|---|
| Scale out | average CPU above 70 % over 5 minutes | add 1 instance | 5 minutes |
| Scale in | average CPU below 30 % over 10 minutes | remove 1 instance | 10 minutes |

- Limits: minimum 1, maximum 3 instances. With `zone_redundant = true`, the default and minimum are 2.
- The cooldown prevents flapping between scale out and scale in.
- The thresholds are example values (assumptions, not measured).
- Do not change the rules in the portal. That causes drift from the code.
