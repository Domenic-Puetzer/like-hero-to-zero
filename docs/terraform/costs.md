# Costs

Back to the [Terraform README](https://github.com/Domenic-Puetzer/like-hero-to-zero/blob/master/terraform/README.md).

Estimate with the Azure Pricing Calculator: euro, region Sweden Central, 730 hours per month. These are estimates, not invoices.

## Items

| Item | Per month |
|---|---|
| App Service S1, 1 instance | 62,26 € |
| MySQL B2ms, 20 GB | 106,79 € |
| Front Door Standard (planned, 50 GB) | 36,02 € |
| Log Analytics, Application Insights, Key Vault | 0 € at small volume |

## Scenarios

| Scenario | With Front Door | Without Front Door |
|---|---|---|
| Normal operation | 205,06 € | 169,05 € |
| Peak, 3 instances all month | 329,58 € | 293,57 € |

- The values with Front Door come from the calculator. The values without Front Door are calculated from the items above (S1 and B2ms only; peak = 3 × 62,26 € + 106,79 €).
- Single items add up to 205,07 € and 329,59 €. The calculator shows 1 cent less, probably due to rounding.
- The deployed setup is the one **without** Front Door.
- "3 instances all month" is the worst case. Autoscale only keeps extra instances during high load.