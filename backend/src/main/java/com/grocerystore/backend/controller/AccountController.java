package com.grocerystore.backend.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.grocerystore.backend.dto.AccountRequest;
import com.grocerystore.backend.dto.AccountResponse;
import com.grocerystore.backend.service.AccountService;
import com.grocerystore.backend.service.AuthService;

@RestController
@RequestMapping("/api/admin/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AuthService authService;

    public AccountController(AccountService accountService, AuthService authService) {
        this.accountService = accountService;
        this.authService = authService;
    }

    @GetMapping
    public List<AccountResponse> getStaffAccounts(HttpSession session) {
        authService.requireAdmin(session);
        return accountService.findStaffAccounts();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createStaffAccount(
            @Valid @RequestBody AccountRequest request,
            HttpSession session) {
        authService.requireAdmin(session);
        return accountService.createStaffAccount(request);
    }
}
