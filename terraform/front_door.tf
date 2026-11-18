/**
 * Optional: Azure Front Door (Standard), only built with enable_front_door.
 *
 * Chain: visitor -> endpoint -> route -> origin group -> origin (the web app).
 * Uses the Standard/Premium resources (azurerm_cdn_frontdoor_*), because
 * Front Door Classic can no longer be created.
 * Not available on Free-Trial and Student subscriptions.
 * When enabled, the web app only accepts traffic from this profile
 * (see ip_restriction in the web app).
 */

/**
 * Front Door profile: the global entry point (CDN and routing).
 * The Standard tier has a base fee per profile, billed hourly.
 */
resource "azurerm_cdn_frontdoor_profile" "main" {
  count               = var.enable_front_door ? 1 : 0
  name                = "afd-${var.project_name}"
  resource_group_name = azurerm_resource_group.main.name
  sku_name            = "Standard_AzureFrontDoor"
}

/**
 * Endpoint: the public hostname visitors use to reach the app via Front Door.
 * The name must be globally unique, hence the random suffix.
 */
resource "azurerm_cdn_frontdoor_endpoint" "main" {
  count                    = var.enable_front_door ? 1 : 0
  name                     = "fde-${var.project_name}-${random_string.suffix.result}"
  cdn_frontdoor_profile_id = azurerm_cdn_frontdoor_profile.main[0].id
}

/**
 * Origin group: the pool of backends behind Front Door.
 *
 * The health probe sends an HTTPS HEAD request to "/" every 100 seconds.
 * An origin that does not answer there is treated as unhealthy.
 * Load balancing uses the default settings.
 */
resource "azurerm_cdn_frontdoor_origin_group" "main" {
  count                    = var.enable_front_door ? 1 : 0
  name                     = "og-${var.project_name}"
  cdn_frontdoor_profile_id = azurerm_cdn_frontdoor_profile.main[0].id

  load_balancing {}

  health_probe {
    path                = "/"
    protocol            = "Https"
    request_type        = "HEAD"
    interval_in_seconds = 100
  }
}

/**
 * Origin: the web app behind Front Door.
 *
 * Requests are forwarded to the app's default hostname over HTTPS (port 443).
 * The same hostname is sent as host header, and the origin certificate name
 * is checked. There is only one origin, so priority and weight have no
 * practical effect.
 */
resource "azurerm_cdn_frontdoor_origin" "main" {
  count                          = var.enable_front_door ? 1 : 0
  name                           = "origin-webapp"
  cdn_frontdoor_origin_group_id  = azurerm_cdn_frontdoor_origin_group.main[0].id
  enabled                        = true
  host_name                      = azurerm_linux_web_app.main.default_hostname
  origin_host_header             = azurerm_linux_web_app.main.default_hostname
  certificate_name_check_enabled = true
  https_port                     = 443
  priority                       = 1
  weight                         = 1000
}

/**
 * Route: connects the endpoint to the origin group.
 *
 * All paths are forwarded to the web app. HTTP requests are redirected to
 * HTTPS, and Front Door talks to the origin over HTTPS only.
 * No caching is configured, so every request is passed through to the app
 * (suitable for dynamic pages).
 */
resource "azurerm_cdn_frontdoor_route" "main" {
  count                         = var.enable_front_door ? 1 : 0
  name                          = "route-webapp"
  cdn_frontdoor_endpoint_id     = azurerm_cdn_frontdoor_endpoint.main[0].id
  cdn_frontdoor_origin_group_id = azurerm_cdn_frontdoor_origin_group.main[0].id
  cdn_frontdoor_origin_ids      = [azurerm_cdn_frontdoor_origin.main[0].id]

  supported_protocols    = ["Http", "Https"]
  patterns_to_match      = ["/*"]
  forwarding_protocol    = "HttpsOnly"
  https_redirect_enabled = true
  link_to_default_domain = true
}
