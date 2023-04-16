package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {
    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    private CreditCardRepository creditCardRepository;
    @Autowired
    private UserRepository userRepository;


    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        CreditCard creditCard = new CreditCard();
        creditCard.setIssuanceBank(payload.getCardIssuanceBank());
        creditCard.setNumber(payload.getCardNumber());
        Optional<User> user = userRepository.findById(payload.getUserId());
        if (user.isPresent()) {
            creditCard.setUser(user.get());
            creditCardRepository.save(creditCard);
            return ResponseEntity.ok(creditCard.getId());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            List<CreditCardView> creditCardViews = user.get().getCreditCards().stream()
                    .map(creditCard -> new CreditCardView(creditCard.getIssuanceBank(), creditCard.getNumber()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(creditCardViews);
        } else {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        Optional<CreditCard> creditCard = creditCardRepository.findByNumber(creditCardNumber);
        if (creditCard.isPresent()) {
            return ResponseEntity.ok(creditCard.get().getUser().getId());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> updateCreditCardBalance(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a transaction of {date: 4/10, amount: 10}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 110}]
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.
        for (UpdateBalancePayload record : payload) {
            Optional<CreditCard> creditCard = creditCardRepository.findByNumber(record.getCreditCardNumber());
            if (creditCard.isPresent()) {
                List<BalanceHistory> balanceHistory = creditCard.get().getBalanceHistory();
                ZonedDateTime zonedDateTime = record.getTransactionTime().atZone(ZoneId.systemDefault());

                // Find the index of the existing BalanceHistory entry with the same date as the transaction
                int index = -1;
                for (int i = 0; i < balanceHistory.size(); i++) {
                    if (balanceHistory.get(i).getDate().equals(zonedDateTime.toInstant())) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    // If no existing BalanceHistory entry with the same date as the transaction, create a new one
                    BalanceHistory newBalanceHistory = new BalanceHistory();
                    newBalanceHistory.setDate(Instant.from(zonedDateTime));
                    newBalanceHistory.setBalance(record.getCurrentBalance());
                    newBalanceHistory.setCreditCard(creditCard.get());
                    balanceHistory.add(newBalanceHistory);

                } else {
                    // If there is an existing BalanceHistory entry with the same date as the transaction, update the balance
                    balanceHistory.get(index).setBalance(record.getCurrentBalance());
                }
                balanceHistory.sort((o1, o2) -> o2.getDate().compareTo(o1.getDate()));
                creditCard.get().setBalanceHistory(balanceHistory);
                creditCardRepository.save(creditCard.get());
            } else {
                return ResponseEntity.badRequest().body("The given card number is not exist.");
            }
        }
        return ResponseEntity.ok().body("Update is done and successful.");
    }
}
