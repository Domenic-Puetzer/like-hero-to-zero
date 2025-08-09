package de.likeherotozero.controller;

import de.likeherotozero.model.EmissionData;
import de.likeherotozero.model.EditRequest;
import de.likeherotozero.service.EmissionDataService;
import de.likeherotozero.service.EditRequestService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid; 
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scientist Controller
 * Handles all scientist-specific functionality including data management,
 * dashboard operations, and peer-review workflow for emission data.
 * Restricted to users with SCIENTIST or ADMIN roles.
 */
@Controller
@RequestMapping("/scientist")
@PreAuthorize("hasRole('SCIENTIST') or hasRole('ADMIN')")
public class ScientistController {

    private final EmissionDataService emissionDataService;
    private final EditRequestService editRequestService;

    /**
     * Constructor injection for required services.
     * @param emissionDataService Service for emission data operations
     * @param editRequestService Service for handling edit requests between scientists
     */
    public ScientistController(EmissionDataService emissionDataService, EditRequestService editRequestService) {
        this.emissionDataService = emissionDataService;
        this.editRequestService = editRequestService;
    }

    /**
     * Displays the scientist dashboard with personal statistics and data overview.
     * Shows user's own data, pending edit requests, and general statistics.
     * @param model Model object for passing data to the view
     * @param auth Authentication object containing user information
     * @return Dashboard template view name
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        String username = auth.getName();
        
        try {
            // Load user-specific data
            List<EmissionData> myData = emissionDataService.getDataByUser(username);
            // Load all data sorted for client-side pagination
            List<EmissionData> recentData = emissionDataService.getAllEmissionsSorted();
            
            // Calculate statistics
            model.addAttribute("totalCountries", emissionDataService.getCountryCount());
            model.addAttribute("totalDataPoints", recentData.size());
            model.addAttribute("myDataCount", myData.size());
            model.addAttribute("latestYear", emissionDataService.getLatestYear());
            
            // Data for tables
            model.addAttribute("myData", myData);
            model.addAttribute("recentData", recentData);
            
            // Load edit requests
            List<EditRequest> pendingRequests = editRequestService.getPendingRequestsForUser(username);
            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("pendingRequestCount", pendingRequests.size());
            
            // Load own edit requests (all statuses)
            List<EditRequest> myEditRequests = editRequestService.getRequestsByRequester(username);
            model.addAttribute("myEditRequests", myEditRequests);
            
            // Available years for filtering (from own data)
            Set<Integer> years = myData.stream()
                .map(EmissionData::getYear)
                .collect(Collectors.toSet());
            model.addAttribute("availableYears", years.stream().sorted(java.util.Collections.reverseOrder()).collect(Collectors.toList()));
            
        } catch (Exception e) {
            model.addAttribute("error", "Fehler beim Laden der Dashboard-Daten: " + e.getMessage());
            // Fallback values
            model.addAttribute("totalCountries", 0);
            model.addAttribute("totalDataPoints", 0);
            model.addAttribute("myDataCount", 0);
            model.addAttribute("latestYear", "---");
            model.addAttribute("myData", List.of());
            model.addAttribute("recentData", List.of());
            model.addAttribute("availableYears", List.of());
        }
        
        return "scientist/dashboard";
    }

    /**
     * Displays the data upload form.
     * Provides a form for scientists to add new emission data entries.
     * @param model Model object for passing data to the view
     * @return Upload form template view name
     */
    @GetMapping("/upload")
    public String uploadForm(Model model) {
        model.addAttribute("emissionData", new EmissionData());
        
        try {
            // Country list for autocomplete/dropdown
            List<String> countries = emissionDataService.getAllCountries();
            model.addAttribute("countries", countries);
        } catch (Exception e) {
            model.addAttribute("countries", List.of());
        }
        
        return "scientist/upload";
    }

    /**
     * Processes data upload form submission.
     * Validates input data and saves new emission entries to the database.
     * @param emissionData The emission data object from the form
     * @param result Validation result object
     * @param redirectAttributes Attributes for flash messages
     * @param auth Authentication object containing user information
     * @return Redirect to appropriate page based on upload result
     */
    @PostMapping("/upload")
    public String uploadData(@Valid @ModelAttribute EmissionData emissionData, 
                           BindingResult result, 
                           RedirectAttributes redirectAttributes,
                           Authentication auth) {
        
        if (result.hasErrors()) {
            return "scientist/upload";
        }

        try {
            // Check if dataset already exists
            if (emissionDataService.existsByCountryAndYear(emissionData.getCountryName(), emissionData.getYear())) {
                redirectAttributes.addFlashAttribute("error", 
                    "Daten für " + emissionData.getCountryName() + " im Jahr " + emissionData.getYear() + " existieren bereits!");
                return "redirect:/scientist/upload";
            }

            // Set metadata
            emissionData.setSourceDate(LocalDate.now());
            emissionData.setDataSource("Scientist Upload - " + auth.getName());
            emissionData.setUploadedBy(auth.getName()); // New field in entity

            // Validation
            if (emissionData.getCo2EmissionKt() < 0) {
                redirectAttributes.addFlashAttribute("error", "CO₂-Werte können nicht negativ sein!");
                return "redirect:/scientist/upload";
            }

            emissionDataService.saveEmission(emissionData);
            redirectAttributes.addFlashAttribute("success", 
                "Daten erfolgreich hinzugefügt: " + emissionData.getCountryName() + " (" + emissionData.getYear() + ")");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler beim Speichern: " + e.getMessage());
            return "redirect:/scientist/upload";
        }

        return "redirect:/scientist/dashboard";
    }

    /**
     * Displays the edit form for a specific emission data entry.
     * Only allows users to edit their own data unless they are admins.
     * @param id The ID of the emission data to edit
     * @param model Model object for passing data to the view
     * @param redirectAttributes Attributes for flash messages
     * @param auth Authentication object containing user information
     * @return Edit form template view name or redirect on error
     */
    @GetMapping("/edit/{id}")
    public String editData(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes, Authentication auth) {
        try {
            EmissionData data = emissionDataService.findById(id);
            
            // Check if user is authorized (only edit own data)
            if (!data.getUploadedBy().equals(auth.getName()) && !auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                redirectAttributes.addFlashAttribute("error", "Sie können nur Ihre eigenen Daten bearbeiten!");
                return "redirect:/scientist/dashboard";
            }
            
            model.addAttribute("emissionData", data);
            model.addAttribute("countries", emissionDataService.getAllCountries());
            return "scientist/edit";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Datensatz nicht gefunden!");
            return "redirect:/scientist/dashboard";
        }
    }

    /**
     * Processes emission data updates via AJAX.
     * Validates user authorization and data integrity before updating.
     * @param id The ID of the emission data to update
     * @param emissionData The updated emission data from the form
     * @param result Validation result object
     * @param auth Authentication object containing user information
     * @return Success message or error description for AJAX response
     */
    @PostMapping("/edit/{id}")
    @ResponseBody
    public String updateData(@PathVariable Long id, 
                           @Valid @ModelAttribute EmissionData emissionData,
                           BindingResult result,
                           Authentication auth) {
        
        if (result.hasErrors()) {
            return "Validierungsfehler in den Eingabedaten";
        }

        try {
            EmissionData existingData = emissionDataService.findById(id);
            
            // Security check
            if (!existingData.getUploadedBy().equals(auth.getName()) && !auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "Sie können nur Ihre eigenen Daten bearbeiten!";
            }

            // Check if new dataset already exists (when changing country/year)
            if (!existingData.getCountryName().equals(emissionData.getCountryName()) || 
                !existingData.getYear().equals(emissionData.getYear())) {
                if (emissionDataService.existsByCountryAndYear(emissionData.getCountryName(), emissionData.getYear())) {
                    return "Daten für " + emissionData.getCountryName() + " im Jahr " + emissionData.getYear() + " existieren bereits!";
                }
            }

            // Update data - FAST with cache update
            existingData.setCountryName(emissionData.getCountryName());
            existingData.setYear(emissionData.getYear());
            existingData.setCo2EmissionKt(emissionData.getCo2EmissionKt());
            existingData.setDataSource(emissionData.getDataSource()); // Respect the entered source!
            existingData.setSourceDate(LocalDate.now());
            
            // Ensure ID is set
            existingData.setId(id);
            
            // Fast save with intelligent cache update - INSTANT!
            emissionDataService.saveEmissionFast(existingData);
            
            return "success";
            
        } catch (Exception e) {
            return "Fehler beim Aktualisieren: " + e.getMessage();
        }
    }

    /**
     * Deletes a specific emission data entry.
     * Only allows users to delete their own data unless they are admins.
     * @param id The ID of the emission data to delete
     * @param redirectAttributes Attributes for flash messages
     * @param auth Authentication object containing user information
     * @return Redirect to dashboard with success or error message
     */
    @PostMapping("/delete/{id}")
    public String deleteData(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication auth) {
        try {
            EmissionData data = emissionDataService.findById(id);
            
            // Security check
            if (!data.getUploadedBy().equals(auth.getName()) && !auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                redirectAttributes.addFlashAttribute("error", "Sie können nur Ihre eigenen Daten löschen!");
                return "redirect:/scientist/dashboard";
            }
            
            emissionDataService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", 
                "Datensatz erfolgreich gelöscht: " + data.getCountryName() + " (" + data.getYear() + ")");
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler beim Löschen: " + e.getMessage());
        }
        
        return "redirect:/scientist/dashboard";
    }

    /**
     * Displays a dedicated view of the user's own emission data.
     * Provides detailed statistics and overview of personal contributions.
     * @param model Model object for passing data to the view
     * @param auth Authentication object containing user information
     * @return My data template view name
     */
    @GetMapping("/my-data")
    public String showMyData(Model model, Authentication auth) {
        try {
            String username = auth.getName();
            List<EmissionData> myData = emissionDataService.getDataByUser(username);
            
            model.addAttribute("myData", myData);
            model.addAttribute("dataCount", myData.size());
            
            // Statistics about own data
            Set<String> myCountries = myData.stream()
                .map(EmissionData::getCountryName)
                .collect(Collectors.toSet());
            model.addAttribute("myCountries", myCountries.size());
            
            Set<Integer> myYears = myData.stream()
                .map(EmissionData::getYear)
                .collect(Collectors.toSet());
            model.addAttribute("myYears", myYears.stream().sorted().collect(Collectors.toList()));
            
        } catch (Exception e) {
            model.addAttribute("error", "Fehler beim Laden Ihrer Daten: " + e.getMessage());
            model.addAttribute("myData", List.of());
        }
        
        return "scientist/my-data";
    }

    /**
     * Handles data export functionality for the user's emission data.
     * Currently provides export preparation, can be extended for actual file generation.
     * @param auth Authentication object containing user information
     * @param redirectAttributes Attributes for flash messages
     * @return Redirect to dashboard with export status message
     */
    @GetMapping("/export")
    public String exportData(Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            String username = auth.getName();
            List<EmissionData> myData = emissionDataService.getDataByUser(username);
            
            if (myData.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Keine Daten zum Exportieren vorhanden!");
            } else {
                redirectAttributes.addFlashAttribute("success", 
                    "Export vorbereitet für " + myData.size() + " Datensätze!");
                // TODO: Implement actual export
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler beim Export: " + e.getMessage());
        }
        
        return "redirect:/scientist/dashboard";
    }

    // ========== EDIT REQUEST CONTROLLERS ==========

    /**
     * Creates an edit request for peer review.
     * Allows scientists to request changes to other scientists' data.
     * @param dataId The ID of the emission data to be edited
     * @param requestMessage Message explaining the requested changes
     * @param proposedCountryName Proposed new country name
     * @param proposedYear Proposed new year
     * @param proposedCo2EmissionKt Proposed new CO2 emission value
     * @param proposedDataSource Proposed new data source
     * @param auth Authentication object containing user information
     * @return Success message or error description for AJAX response
     */
    @PostMapping("/request-edit")
    @ResponseBody
    public String requestEdit(@RequestParam Long dataId,
                            @RequestParam String requestMessage,
                            @RequestParam String proposedCountryName,
                            @RequestParam Integer proposedYear,
                            @RequestParam Double proposedCo2EmissionKt,
                            @RequestParam String proposedDataSource,
                            Authentication auth) {
        try {
            editRequestService.createEditRequest(
                dataId, 
                auth.getName(), 
                requestMessage, 
                proposedCountryName, 
                proposedYear, 
                proposedCo2EmissionKt, 
                proposedDataSource
            );
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    /**
     * Approves an edit request from another scientist.
     * Applies the proposed changes and notifies the requester.
     * @param requestId The ID of the edit request to approve
     * @param responseMessage Response message to the requester
     * @param redirectAttributes Attributes for flash messages
     * @return Redirect to dashboard with approval status
     */
    @PostMapping("/approve-edit/{requestId}")
    public String approveEditRequest(@PathVariable Long requestId,
                                   @RequestParam String responseMessage,
                                   RedirectAttributes redirectAttributes) {
        try {
            editRequestService.approveEditRequest(requestId, responseMessage);
            redirectAttributes.addFlashAttribute("success", "Bearbeitungsanfrage wurde genehmigt!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler: " + e.getMessage());
        }
        return "redirect:/scientist/dashboard";
    }

    /**
     * Rejects an edit request from another scientist.
     * Declines the proposed changes and notifies the requester.
     * @param requestId The ID of the edit request to reject
     * @param responseMessage Response message explaining the rejection
     * @param redirectAttributes Attributes for flash messages
     * @return Redirect to dashboard with rejection status
     */
    @PostMapping("/reject-edit/{requestId}")
    public String rejectEditRequest(@PathVariable Long requestId,
                                  @RequestParam String responseMessage,
                                  RedirectAttributes redirectAttributes) {
        try {
            editRequestService.rejectEditRequest(requestId, responseMessage);
            redirectAttributes.addFlashAttribute("success", "Bearbeitungsanfrage wurde abgelehnt!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler: " + e.getMessage());
        }
        return "redirect:/scientist/dashboard";
    }
}