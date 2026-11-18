variable "location" {
  type        = string
  description = "Azure region where all resources are created"
  default     = "swedencentral"
}

variable "project_name" {
  type        = string
  description = "Name prefix used for all resources"
  default     = "likeherotozero"
}

variable "app_service_sku" {
  type        = string
  description = "App Service Plan SKU (e.g. B1, S1, P1v3); autoscale rules are created above B1"
  default     = "S1"
}

variable "mysql_sku" {
  type        = string
  description = "SKU of the MySQL Flexible Server (e.g. B_Standard_B1ms, B_Standard_B2ms)"
  default     = "B_Standard_B2ms"
}

variable "db_username" {
  type        = string
  description = "Admin username for the MySQL server (no default)"
  sensitive   = true
}

variable "db_password" {
  type        = string
  description = "Admin password for the MySQL server (no default)"
  sensitive   = true
}

variable "alert_email" {
  type        = string
  description = "E-mail address that receives monitoring alerts (no default)"
  sensitive   = true
}

variable "enable_front_door" {
  type        = bool
  description = "Build Front Door and restrict the web app to it (not available on Free-Trial and Student subscriptions, so disabled by default)"
  default     = false
}

variable "zone_redundant" {
  type        = bool
  description = "Spread the App Service plan over availability zones (needs a Premium SKU, 2+ instances and quota for zone-redundant instances)"
  default     = false
}