package com.bank.api.service;

import com.bank.api.domain.Account;
import com.bank.api.domain.User;
import com.bank.api.dto.request.CreateAccountRequest;
import com.bank.api.dto.response.AccountResponse;
import com.bank.api.exception.ResourceNotFoundException;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles account creation and retrieval.
 *
 * DESIGN DECISIONS:
 *
 * 1. Account number generation — server-side, never client-provided.
 *    Format: "GB" + 18 random digits. Simple for a take-home exercise.
 *    PRODUCTION NOTE: Real sort codes and account numbers follow
 *    strict formats (UK: 6-digit sort code + 8-digit account number).
 *    Generation would be handled by a dedicated number allocation service
 *    to guarantee uniqueness at scale without DB collisions.
 *
 * 2. Ownership always scoped to userId — findByIdAndOwnerId used throughout.
 *    A user can never access another user's account even if they guess the UUID.
 *
 * 3. @Transactional on writes — account creation involves linking to a User.
 *    If anything fails mid-operation the whole thing rolls back.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new account for the authenticated user.
     */
    @Transactional
    public AccountResponse createAccount(UUID userId, CreateAccountRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .accountType(request.accountType())
                .owner(owner)
                .build();

        owner.addAccount(account);
        accountRepository.save(account);

        return AccountResponse.fromAccount(account);
    }

    /**
     * Returns all accounts belonging to the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts(UUID userId) {
        return accountRepository.findAllByOwner_Id(userId)
                .stream()
                .map(AccountResponse::fromAccount)
                .collect(Collectors.toList());
    }

    /**
     * Returns a specific account — only if owned by the authenticated user.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId, UUID userId) {
        Account account = accountRepository.findByIdAndOwner_Id(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return AccountResponse.fromAccount(account);
    }

    /**
     * Loads an Account entity for internal use by TransactionService.
     * Verifies ownership. Returns the entity, not a DTO.
     *
     * DECISION: Package-accessible method returning an entity.
     * WHY: TransactionService needs the actual Account entity to call
     * deposit()/withdraw() on it and link transactions to it.
     * Returning a DTO would lose the JPA managed entity reference.
     */
    @Transactional(readOnly = true)
    public Account loadAccountForOwner(UUID accountId, UUID userId) {
        return accountRepository.findByIdAndOwner_Id(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    /**
     * Generates a unique account number.
     * Retries if a collision occurs (extremely unlikely but handled correctly).
     *
     * PRODUCTION NOTE: At scale this naive approach can cause collisions.
     * A real system uses a dedicated sequence table or external service.
     */
    private String generateAccountNumber() {
        String number;
        do {
            long randomPart = (long) (Math.random() * 1_000_000_000_000_000_000L);
            number = String.format("GB%018d", Math.abs(randomPart));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
