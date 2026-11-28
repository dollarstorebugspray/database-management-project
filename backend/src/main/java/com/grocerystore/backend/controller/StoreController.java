package com.grocerystore.backend.controller;

import java.util.List;

import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.grocerystore.backend.dto.RegionResponse;
import com.grocerystore.backend.dto.StoreResponse;
import com.grocerystore.backend.service.StoreService;

@Validated
@RestController
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/api/stores")
    public List<StoreResponse> getStores() {
        return storeService.findStores();
    }

    @GetMapping("/api/stores/{storeId}")
    public StoreResponse getStore(@PathVariable @Min(1) Integer storeId) {
        return storeService.findStore(storeId);
    }

    @GetMapping("/api/regions")
    public List<RegionResponse> getRegions() {
        return storeService.findRegions();
    }
}
