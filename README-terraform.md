# Like Hero To Zero – Cloud Infrastructure ☁️

**Portfolio Assignment:**  
Infrastructure as Code (Terraform) that hosts the Like Hero To Zero web portal on Microsoft Azure. This file covers the cloud infrastructure only. The application itself is described in the [main README](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/README.md).

The portal shows global CO₂ emissions. The Terraform script builds everything it needs in the Azure cloud: a server, a database, a protected place for passwords, and monitoring. One command creates the environment, one command removes it again.

**Fictional case study:** *ClimateLens GmbH* runs the portal. Users come from all over the world (60 % Europe, 25 % North America, 15 % rest). Load peaks during climate summits. The team has 3 people and a fictional budget of 300 € per month.

---

## 📝 Documentation

- [docs/terraform/architecture.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/architecture.md) – Overview, resource names, request and secret flow
- [docs/terraform/app-service.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/app-service.md) – App Service plan, web app, autoscale
- [docs/terraform/database.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/database.md) – MySQL Flexible Server
- [docs/terraform/key-vault.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/key-vault.md) – Secret management in detail
- [docs/terraform/monitoring.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/monitoring.md) – Logs, alert, budget
- [docs/terraform/front-door.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/front-door.md) – Optional Front Door and zone redundancy
- [docs/terraform/costs.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/costs.md) – Cost estimate

---

## 🚀 Getting Started

### Prerequisites

- Azure account with an active subscription (permission to assign roles, because the code creates two role assignments)
- [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)
- [Terraform](https://developer.hashicorp.com/terraform/install) 1.5 or newer
- Java 21 and Maven
- Enough quota for the chosen SKUs in the chosen region

Check the tools:

```
az version
terraform version
java -version
mvn -v
```

#### 1. Clone the repository

```
git clone https://github.com/Domenic-Puetzer/like-hero-to-zero.git
cd like-hero-to-zero
```

#### 2. Sign in to Azure

```
az login
az account show
```

`az account show` prints the subscription Terraform will use.

#### 3. Set the secrets as environment variables

Replace the placeholders. The variables only exist in the current terminal window.

**PowerShell:**

```powershell
$env:TF_VAR_db_username = '<username>'
$env:TF_VAR_db_password = '<strong-password>'
$env:TF_VAR_alert_email = '<email-address>'
```

**or CMD:**

```cmd
set TF_VAR_db_username=<username>
set TF_VAR_db_password=<strong-password>
set TF_VAR_alert_email=<email-address>
```

- In PowerShell, use single quotes. Double quotes expand `$` characters in a password.
- In CMD, do not use quotes (they become part of the value) and avoid `& ^ % < > |` in the password.

#### 4. Create the infrastructure

```
cd terraform
terraform init
terraform validate
terraform plan
terraform apply
```

- `init` downloads the providers.
- `validate` checks the syntax.
- `plan` shows what will be created (21 resources).
- `apply` creates everything. Confirm with `yes`.

#### 5. Build the application

Back in the project root:

```
cd ..
mvn clean package -DskipTests
```

#### 6. Deploy the JAR file

The JAR file is not part of Terraform. Until it is deployed, the web app stays empty. First look up the app name (it looks like `app-likeherotozero-<suffix>`):

```
az webapp list --resource-group rg-likeherotozero --query "[].name" --output tsv
```

Then deploy (same command in PowerShell and CMD):

```
az webapp deploy --resource-group rg-likeherotozero --name <APP-NAME> --src-path target\likeherotozero-0.0.1-SNAPSHOT.jar --type jar
```

#### 7. Open the app

On the first start the application imports the data (`DataLoader`). This took about 2 minutes on S1 and B2ms (one run, measured with a stopwatch). Wait for it before opening the app.

The URL was printed in step 4. To show it again:

```
terraform -chdir=terraform output app_url
```

#### 8. Clean up

```
cd terraform
terraform destroy
```

Confirm with `yes`. Terraform deletes the resource group and everything in it, including the deployed application. Check that it is gone (prints `false`):

```
az group exists --name rg-likeherotozero
```

Remove the environment variables:

**PowerShell:**

```powershell
Remove-Item Env:TF_VAR_db_username, Env:TF_VAR_db_password, Env:TF_VAR_alert_email
```

**or CMD:**

```cmd
set TF_VAR_db_username=
set TF_VAR_db_password=
set TF_VAR_alert_email=
```

---

## 🔧 Technologies

- Terraform (>= 1.5) with the providers `azurerm` (~> 4.0) and `random` (~> 3.0)
- Azure App Service (Linux, Java 21)
- Azure Database for MySQL Flexible Server 8.4
- Azure Key Vault with Managed Identity
- Azure Monitor (Log Analytics, Application Insights, alerts, autoscale, budget)
- Azure Front Door Standard (optional)
- Azure CLI (deployment of the JAR file)

---

## 📦 Features

- 21 resources (26 with Front Door), region Sweden Central
- Autoscale from 1 to 3 instances: one more above 70 % CPU, one less below 30 % CPU
- No credentials in code, files or app settings: secrets live in the Key Vault, the app reads them through its Managed Identity
- Least privilege: Terraform may write secrets, the web app may only read them
- Monitoring: logs and metrics in Log Analytics, alert on more than 5 HTTP 5xx errors in 5 minutes, budget warning at 80 %
- Two switches in `variables.tf`, both off by default: `enable_front_door` and `zone_redundant`

---

## 🛠️ To Be Done

- Front Door and zone redundancy: prepared in the code, not deployed (the student account cannot create Front Door, error 400, and has no quota for `P1v3`)
- Private endpoint for the database
- Remote state instead of a local state file
- Real JDBC batching in the `DataLoader`
- CI/CD pipeline for the JAR deployment

---

## 📁 Project Structure

```
terraform/
 ├─ main.tf                # Resource group, random suffix
 ├─ providers.tf           # Terraform and provider versions, azurerm settings
 ├─ app_service.tf         # App Service plan, Linux web app, autoscale
 ├─ database.tf            # MySQL Flexible Server, database, firewall rule
 ├─ key_vault.tf           # Key Vault, role assignments, secrets
 ├─ monitoring.tf          # Log Analytics, Application Insights, alert, budget
 ├─ front_door.tf          # Optional Front Door (enable_front_door)
 ├─ variables.tf           # Input variables
 ├─ output.tf              # Outputs: app_url, front_door_url
 └─ .terraform.lock.hcl    # Exact provider versions (commit this file)

docs/terraform/            # One short explanation per building block
```

Terraform reads all `.tf` files of the folder together. The split into several files is only for readability.

Created locally, never commit: `.terraform/` and `terraform.tfstate` (the state file contains values in plain text).

---

## 🧑‍💻 Development

### Variables (`variables.tf`)

| Variable | Type | Meaning | Default |
|---|---|---|---|
| `location` | string | Azure region for all resources | `swedencentral` |
| `project_name` | string | Name prefix for all resources (max. 15 characters, because of the 24-character Key Vault name) | `likeherotozero` |
| `app_service_sku` | string | App Service plan SKU (e.g. B1, S1, P1v3). Autoscale is only created above B1 | `S1` |
| `mysql_sku` | string | MySQL SKU (e.g. `B_Standard_B1ms`, `B_Standard_B2ms`) | `B_Standard_B2ms` |
| `db_username` | string, sensitive | Admin user name of the MySQL server | no default |
| `db_password` | string, sensitive | Admin password of the MySQL server | no default |
| `alert_email` | string, sensitive | E-mail address for alerts and budget warning | no default |
| `enable_front_door` | bool | Build Front Door and restrict the web app to it | `false` |
| `zone_redundant` | bool | Spread the App Service plan over availability zones | `false` |

Fixed in the code, not variables: MySQL version 8.4, storage 20 GB, database name `likeherotozero`, autoscale limits and thresholds, alert threshold, budget amount (billing currency) and its start date `2026-11-01`. The thresholds and the budget are example values.

### Change a setting

**PowerShell:**

```powershell
$env:TF_VAR_app_service_sku = 'B1'
```

**or CMD:**

```cmd
set TF_VAR_app_service_sku=B1
```

Or directly in the command:

```
cd terraform
terraform plan -var="app_service_sku=B1"
```

### Secret management in short

1. The three values are passed as environment variables (`TF_VAR_...`). They are in no file.
2. The variables are `sensitive` and have no default.
3. Terraform writes `db_username` and `db_password` as secrets into the Key Vault. `alert_email` is not stored there. It is only used for the alert and the budget.
4. The web app has a Managed Identity with the role *Key Vault Secrets User*. Terraform uses *Key Vault Secrets Officer*.
5. The app settings only contain a reference (`@Microsoft.KeyVault(SecretUri=...)`), no secret value.

Details: [key-vault.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/key-vault.md).

### Enable Front Door and zone redundancy

Only works on a subscription without the student restrictions (needs Front Door and quota for a Premium SKU).

**PowerShell:**

```powershell
$env:TF_VAR_enable_front_door = 'true'
$env:TF_VAR_zone_redundant = 'true'
$env:TF_VAR_app_service_sku = 'P1v3'
```

**or CMD:**

```cmd
set TF_VAR_enable_front_door=true
set TF_VAR_zone_redundant=true
set TF_VAR_app_service_sku=P1v3
```

Then apply:

```
cd terraform
terraform plan
terraform apply
```

The two switches are independent. With Front Door, the public address is the output `front_door_url`.

### Update after changes

- Infrastructure changed: `cd terraform`, then `terraform plan` and `terraform apply`.
- Application changed: repeat steps 5 and 6 of [Getting Started](#-getting-started).

---

## 💶 Cost (estimate)

Azure Pricing Calculator, euro, Sweden Central, 730 hours per month. These are estimates, not invoices.

| Scenario | Per month |
|---|---|
| Normal operation (S1, B2ms, Front Door) | 205,06 € |
| Peak, 3 instances all month (with Front Door) | 329,58 € |

Details and the numbers without Front Door: [costs.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/costs.md).

---

## 📄 License

This project is provided for educational purposes as part of a university case study.
**All rights reserved.**
Use, distribution, or modification of the code is only permitted for university staff and examiners for review purposes.
Any other use is prohibited without explicit permission.