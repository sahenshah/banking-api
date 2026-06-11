package com.bank.api.service;

import com.bank.api.domain.Account;
import com.bank.api.domain.User;
import com.bank.api.dto.request.CreateAccountRequest;
import com.bank.api.dto.response.AccountResponse;
import com.bank.api.exception.AccessDeniedException;
import com.bank.api.exception.ResourceNotFoundException;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.bank.api.dto.request.UpdateAccountRequest;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

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

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts(UUID userId) {
        return accountRepository.findAllByOwner_Id(userId)
                .stream()
                .map(AccountResponse::fromAccount)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId, UUID userId) {
        Account account = findAccountWithOwnershipCheck(accountId, userId);
        return AccountResponse.fromAccount(account);
    }

    /**
     * Loads an Account entity for internal use by TransactionService.
     * Checks existence first (404), then ownership (403).
     */
    @Transactional(readOnly = true)
    public Account loadAccountForOwner(UUID accountId, UUID userId) {
        return findAccountWithOwnershipCheck(accountId, userId);
    }

    /**
     * Central ownership check used by all account access methods.
     *
     * DECISION: Check existence first, then ownership.
     * WHY: The spec has separate scenarios:
     * - Account doesn't exist → 404 Not Found
     * - Account belongs to another user → 403 Forbidden
     * We must distinguish these two cases.
     */
    private Account findAccountWithOwnershipCheck(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.isOwnedBy(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        return account;
    }

    private String generateAccountNumber() {
        String number;
        do {
            long randomPart = (long) (Math.random() * 1_000_000_000_000_000_000L);
            number = String.format("GB%018d", Math.abs(randomPart));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    @Transactional
    public AccountResponse updateAccount(UUID accountId, UUID userId,
                                         UpdateAccountRequest request) {
        Account account = findAccountWithOwnershipCheck(accountId, userId);

        if (request.accountType() != null) {
            account.setAccountType(request.accountType());
        }

        accountRepository.save(account);
        return AccountResponse.fromAccount(account);
    }

    @Transactional
    public void deleteAccount(UUID accountId, UUID userId) {
        Account account = findAccountWithOwnershipCheck(accountId, userId);
        accountRepository.delete(account);
    }
}