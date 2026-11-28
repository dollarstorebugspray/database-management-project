package com.grocerystore.backend.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.backend.dto.SalespersonRequest;
import com.grocerystore.backend.dto.SalespersonResponse;
import com.grocerystore.backend.model.SessionUser;
import com.grocerystore.backend.service.AuthService;
import com.grocerystore.backend.service.SalespersonService;

@Validated
@RestController
@RequestMapping("/api/admin/salespeople")
public class SalespersonController {

    private final SalespersonService salespersonService;
    private final AuthService authService;

    public SalespersonController(SalespersonService salespersonService, AuthService authService) {
        this.salespersonService = salespersonService;
        this.authService = authService;
    }

    @GetMapping
    public List<SalespersonResponse> getSalespeople(
            @RequestParam(required = false) @Min(1) Integer storeId,
            HttpSession session) {
        SessionUser user = authService.requireStaff(session);
        List<SalespersonResponse> salespeople = salespersonService.findByStore(storeId);
        if (user.isAdmin()) {
            return salespeople;
        }
        return salespeople.stream().map(SalespersonResponse::withoutSalary).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalespersonResponse createSalesperson(
            @Valid @RequestBody SalespersonRequest request,
            HttpSession session) {
        authService.requireAdmin(session);
        return salespersonService.create(request);
    }

    @PutMapping("/{salespersonId}")
    public SalespersonResponse updateSalesperson(
            @PathVariable @Min(1) Integer salespersonId,
            @Valid @RequestBody SalespersonRequest request,
            HttpSession session) {
        authService.requireAdmin(session);
        return salespersonService.update(salespersonId, request);
    }
}
