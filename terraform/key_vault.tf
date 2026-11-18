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
