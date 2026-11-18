# Monitoring

File: `monitoring.tf`. Back to the [Terraform README](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/terraform/README.md).

## What it does

It collects logs and metrics in one place, notifies by e-mail on server errors, and warns before the budget is used up.

## Building blocks

| Resource | Name | Purpose |
|---|---|---|
| Log Analytics workspace | `log-<project_name>` | Central store for logs and metrics. `PerGB2018` (pay per ingested GB), 30 days retention |
| Application Insights | `appi-<project_name>` | Monitors the Java application: requests, errors, response times. Stores its data in the workspace |
| Diagnostic settings | `diag-web-app`, `diag-key-vault`, `diag-mysql` | Send all log categories (`allLogs`) of the three services to the workspace |
| Action group | `ag-<project_name>` | Defines who is notified: one e-mail receiver (`alert_email`) |
| Metric alert | `alert-http-5xx` | Fires on server errors |
| Budget | `budget-<project_name>` | Warns about costs |

The Key Vault log also records each access to a secret.

## Alert

- Metric: `Http5xx` of the web app, aggregation `Total`
- Condition: more than 5 within 5 minutes
- Evaluated every minute, severity 2
- Notification: e-mail through the action group
- The threshold is an example value (assumption, not measured).

Alert rules and action groups are global resources and have no region.

## Budget

| Setting | Value |
|---|---|
| Scope | Resource group |
| Amount | 10 (in the billing currency, example value) |
| Period | monthly, start date `2026-11-01` |
| Notification | at 80 % of actual costs, e-mail to `alert_email` |

- The budget only warns. It does not stop any spending.
- The start date must be the first day of a month. It is fixed in the code and may need an update for a later rebuild.

## Web app logs

The web app also writes HTTP logs to its own file system (see [app-service.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/app-service.md)).