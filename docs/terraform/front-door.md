# Front Door and Zone Redundancy (optional)

File: `front_door.tf` and the `zone_redundant` switch in `app_service.tf`. Back to the [Terraform README](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/terraform/README.md).

Both features are **prepared in the code but not deployed**. Both switches are `false` by default.

## Why not deployed

| Feature | Reason |
|---|---|
| Front Door (`enable_front_door`) | The student account cannot create Front Door (error 400). The variable description says the same for Free-Trial subscriptions |
| Zone redundancy (`zone_redundant`) | Needs a Premium SKU such as `P1v3`. The student account has no quota for it |

## Front Door

Standard tier (`Standard_AzureFrontDoor`), built with the `azurerm_cdn_frontdoor_*` resources, because Front Door Classic can no longer be created. 5 resources:

```
visitor -> endpoint -> route -> origin group -> origin (web app)
```

| Resource | Name | Notes |
|---|---|---|
| Profile | `fd-<project_name>` | Global entry point. The Standard tier has a base fee per profile, billed hourly |
| Endpoint | `fde-<project_name>-<suffix>` | Public host name. Must be globally unique |
| Origin group | `og-<project_name>` | Health probe: HTTPS `HEAD` on `/` every 100 seconds. Default load balancing |
| Origin | `origin-webapp` | The web app, over HTTPS port 443. Certificate name is checked |
| Route | `route-webapp` | All paths (`/*`), HTTP redirected to HTTPS, HTTPS only to the origin |

- No caching is configured. Every request is passed through to the app.
- There is only one origin in one region (Sweden Central).
- No latency measurements exist.

With Front Door enabled, the web app accepts only traffic from this profile (see [app-service.md](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/docs/terraform/app-service.md)). The public address is the output `front_door_url`.

## Zone redundancy

With `zone_redundant = true`:

- The plan sets `zone_balancing_enabled = true` and `worker_count = 2`.
- Autoscale uses a minimum of 2 instances (maximum stays 3).
- Needs a Premium SKU, 2 or more instances, and quota for zone-redundant instances.

## How to enable

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

```
cd terraform
terraform plan
terraform apply
```

The switches are independent of each other.
