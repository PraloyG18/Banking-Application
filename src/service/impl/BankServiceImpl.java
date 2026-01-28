package service.impl;

import domsin.Account;
import domsin.Customer;
import domsin.Transaction;
import domsin.Type;
import exceptions.AccountNotFoundException;
import exceptions.InsufficientFundsException;
import exceptions.ValidationException;
import repository.AccountRepository;
import repository.CustomerRepository;
import repository.TransactionRepository;
import service.BankService;
import util.Validation;

import java.time.LocalDateTime;
import java.util.*;

import java.util.UUID;
import java.util.stream.Collectors;

public class BankServiceImpl implements BankService{

    private final AccountRepository accountRepository = new AccountRepository();
    private final TransactionRepository transactionRepository = new TransactionRepository();
    private final CustomerRepository customerRepository = new CustomerRepository();

    private final Validation<String> validateName = name -> {
        if (name == null || name.isBlank()) throw new ValidationException("Name is required");
    };

    private final Validation<String> validateEmail = email -> {
        if (email == null || !email.contains("@")) throw new ValidationException("Email is required");
    };

    private final Validation<String> validateType = type -> {
        if (type == null || !(type.equalsIgnoreCase("SAVINGS") || type.contains("CURRENT ")))
            throw new ValidationException("Type must be savings or current");
    };

    private final Validation<Double> validateAmountPositive = amount -> {
        if (amount == null || amount < 0)
            throw new ValidationException("Please enter a valid amount");
    };

    @Override
    public String openAccount(String name, String email, String accountType) {
        validateName.validate(name);
        validateEmail.validate(email);
        validateType.validate(accountType);
        String customerId = UUID.randomUUID().toString();

        // Create Customer

        Customer c = new Customer(customerId, name, email);
        customerRepository.save(c);

        String accountNumber = getAccountNumber();
        // Change later
    //    String accountNumber = UUID.randomUUID().toString();
        //Account account = new Account(accountNumber, accountType, (double)0, customerId);
        Account account = new Account(accountNumber, customerId, (double)0, accountType);

        accountRepository.save(account);
        return accountNumber;
    }

    @Override
    public List<Account> listAccounts() {
        return accountRepository.findAll().stream().
                sorted(Comparator.comparing(Account::getAccountNumber))
                .collect(Collectors.toList());
    }

    @Override
    public void deposit(String accountNumber, Double amount, String note) {
        validateAmountPositive.validate(amount);
        //accountNumber
        Account account = accountRepository.findByNumber(accountNumber).
                orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        account.setBalance(account.getBalance() + amount);
        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                Type.Deposit,
                account.getAccountNumber(),
                amount,
                LocalDateTime.now(),
                note
        );
        transactionRepository.add(transaction);
    }

    @Override
    public void withdraw(String accountNumber, Double amount, String note) {
        validateAmountPositive.validate(amount);

        Account account = accountRepository.findByNumber(accountNumber).
                orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        if(account.getBalance().compareTo(amount) < 0)
            throw new InsufficientFundsException("Insufficient Balance");
        account.setBalance(account.getBalance() - amount);
        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                Type.Withdraw,
                account.getAccountNumber(),
                amount,
                LocalDateTime.now(),
                note
        );
        transactionRepository.add(transaction);
    }

    @Override
    public void transfer(String fromAcc, String toAcc, Double amount, String note) {
        validateAmountPositive.validate(amount);
        
        if(fromAcc.equals(toAcc))
            throw new ValidationException("Cannot transfer to your own account");

        Account from = accountRepository.findByNumber(fromAcc).
                orElseThrow(() -> new AccountNotFoundException("Account not found: " + fromAcc));

        Account to = accountRepository.findByNumber(toAcc).
                orElseThrow(() -> new AccountNotFoundException("Account not found: " + toAcc));

        if(from.getBalance().compareTo(amount) < 0)
            throw new InsufficientFundsException("Insufficient Balance");
        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        transactionRepository.add(new Transaction(
                UUID.randomUUID().toString(),   // id
                Type.Transfer_Out,              // type
                from.getAccountNumber(),        // accountNumber
                amount,                         // amount
                LocalDateTime.now(),            // timestamp
                note                            // note
        ));

        transactionRepository.add(new Transaction(
                UUID.randomUUID().toString(),   // id
                Type.Transfer_In,               // type
                to.getAccountNumber(),          // accountNumber
                amount,                         // amount
                LocalDateTime.now(),            // timestamp
                "Transfer from " + from.getAccountNumber()
        ));

    }

    @Override
    public List<Transaction> getStatement(String account) {
        return transactionRepository.findByAccount(account).stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp))
                .collect(Collectors.toList());
    }

    @Override
    public List<Account> searchAccountsByCustomerName(String q) {
        String query = (q == null) ? "": q.toLowerCase();
        /*List<Account> result = new ArrayList<>();
        for (Customer c : customerRepository.findAll()){
            if(c.getName().toLowerCase().contains(query))
                result.addAll(accountRepository.findByCustomerId(c.getId()));
        }
        result.sort(Comparator.comparing(Account::getAccountNumber));
        return result;
    */

        return customerRepository.findAll().stream()
                .filter(c -> c.getName().toLowerCase().contains(query))
                .flatMap(c -> accountRepository.findByCustomerId(c.getId()).stream())
                .sorted(Comparator.comparing(Account::getAccountNumber))
                .collect(Collectors.toList());
    }

    private String getAccountNumber(){
        int size = accountRepository.findAll().size() + 1;
        return String.format("AC%06d", size);
    }
}