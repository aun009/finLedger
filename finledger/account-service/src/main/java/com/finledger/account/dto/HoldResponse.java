package com.finledger.account.dto;

import com.finledger.account.entity.Account;
import com.finledger.account.entity.BalanceHold;
import java.math.BigDecimal;

public record HoldResponse(String holdKey, String status, BigDecimal amount, AccountResponse account) {
    public static HoldResponse from(BalanceHold hold, Account account) {
        return new HoldResponse(hold.getHoldKey(), hold.getStatus().name(), hold.getAmount(), AccountResponse.from(account));
    }
}
