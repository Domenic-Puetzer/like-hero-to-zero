# Key Vault and Secret Management

File: `key_vault.tf`. Back to the [Terraform README](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/terraform/README.md).

## Flow

1. The values are passed as environment variables: `TF_VAR_db_username`, `TF_VAR_db_password`, `TF_VAR_alert_email`.
2. They are read into `sensitive` variables without a default. Terraform hides them in its console output.
3. Terraform writes `db_username` and `db_password` as secrets into the Key Vault.
4. The web app has a Managed Identity. With it, the app signs in to Azure without a password.
5. The app settings contain only a reference. Azure resolves it at runtime.

## Variables

| Variable | Where the value goes |
|---|---|
| `db_username` | Secret `db-username` in the Key Vault, admin login of the MySQL server |
| `db_password` | Secret `db-password` in the Key Vault, admin password of the MySQL server |
| `alert_email` | Receiver of the action group and the budget warning. Not stored in the Key Vault |

All three have no default. If one is missing, Terraform asks for it or stops with an error. There is no hidden fallback value.

## Key Vault (`azurerm_key_vault`)

| Setting | Value |
|---|---|
| Name | `kv-<project_name>-<suffix>` (max. 24 characters) |
| SKU | `standard` |
| Authorization | Azure roles (RBAC) instead of access policies |
| Soft delete | 7 days |

## Roles (least privilege)

| Who | Role | Purpose |
|---|---|---|
| User that runs Terraform | Key Vault Secrets Officer | Terraform may write the secrets |
| Managed Identity of the web app | Key Vault Secrets User | The app may only read secrets |

The secrets are created only after the role assignment exists (`depends_on`).

## What the app settings contain

| Setting | Content | Contains a secret? |
|---|---|---|
| `DB_USERNAME` | `@Microsoft.KeyVault(SecretUri=...)` | no, only a reference |
| `DB_PASSWORD` | `@Microsoft.KeyVault(SecretUri=...)` | no, only a reference |
| `DB_NAME` | database name | no |
| `SPRING_DATASOURCE_URL` | JDBC URL with `sslMode=REQUIRED` | no |
| `APPLICATIONINSIGHTS_CONNECTION_STRING` | telemetry connection string | no password |

## Audit trail

The Key Vault sends all logs to Log Analytics (`diag-key-vault`). Each access to a secret is recorded there.