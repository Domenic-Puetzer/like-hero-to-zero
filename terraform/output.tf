output "app_url" {
  description = "Public URL of the web app"
  value       = "https://${azurerm_linux_web_app.main.default_hostname}"
}

output "front_door_url" {
  description = "Public URL of the Front Door endpoint (only if enable_front_door = true)"
  value       = length(azurerm_cdn_frontdoor_endpoint.main) > 0 ? "https://${azurerm_cdn_frontdoor_endpoint.main[0].host_name}" : "Front Door is disabled"
}
