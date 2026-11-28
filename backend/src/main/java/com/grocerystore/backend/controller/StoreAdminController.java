package com.grocerystore.backend.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.backend.dto.RegionRequest;
import com.grocerystore.backend.dto.RegionResponse;
import com.grocerystore.backend.dto.StoreRequest;
import com.grocerystore.backend.dto.StoreResponse;
import com.grocerystore.backend.service.AuthService;
import com.grocerystore.backend.service.StoreService;

@Validated
@RestController
public class StoreAdminController {

    private final StoreService storeService;
    private final AuthService authService;

    public StoreAdminController(StoreService storeService, AuthService authService) {
        this.storeService = storeService;
        this.authService = authService;
    }

    @PostMapping("/api/admin/stores")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse createStore(
            @Valid @RequestBody StoreRequest request,
            HttpSession session) {
        authService.requireAdmin(session);
        return storeService.createStore(request);
    }

    @PutMapping("/api/admin/stores/{storeId}")
    public StoreResponse updateStore(
            @PathVariable @Min(1) Integer storeId,
            @Valid @RequestBody StoreRequest request,
            HttpSession session) {
        authService.requireAdmin(session);
        return storeService.updateStore(storeId, request);
    }

    @PostMapping("/api/admin/regions")
    @ResponseStatus(HttpStatus.CREATED)
    public RegionResponse createRegion(
            @Valid @RequestBody RegionRequest request,
            HttpSession session) {
        authService.requireAdmin(session);
        return storeService.createRegion(request);
    }

    @PutMapping("/api/admin/regions/{regionId}")
    public RegionResponse updateRegion(
            @PathVariable @Min(1) Integer regionId,
            @Valid @RequestBody RegionRequest request,
            HttpSession session) {
        authService.requireAdmin(session);
        return storeService.updateRegion(regionId, request);
    }
}
