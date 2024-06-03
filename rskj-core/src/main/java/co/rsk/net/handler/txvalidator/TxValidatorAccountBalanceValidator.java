/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.net.handler.txvalidator;

import co.rsk.core.Coin;
import co.rsk.core.bc.ClaimTransactionValidator;
import co.rsk.net.TransactionValidationResult;
import org.ethereum.config.Constants;
import org.ethereum.core.SignatureCache;
import org.ethereum.core.Transaction;
import org.ethereum.core.ValidationArgs;

import java.math.BigInteger;

/**
 * Checks if an account can pay the transaction execution cost
 */
public class TxValidatorAccountBalanceValidator implements TxValidatorStep {

    private final ClaimTransactionValidator claimTransactionValidator;

    public TxValidatorAccountBalanceValidator(
            Constants constants,
            SignatureCache signatureCache) {
        this.claimTransactionValidator = new ClaimTransactionValidator(signatureCache, constants);
    }

    @Override
    public TransactionValidationResult validate(Transaction tx, ValidationArgs validationArgs, BigInteger gasLimit, Coin minimumGasPrice, long bestBlockNumber, boolean isFreeTx) {
        if (isFreeTx) {
            return TransactionValidationResult.ok();
        }

        if (validationArgs.getAccountState() == null) {
            return TransactionValidationResult.withError("the sender account doesn't exist");
        }

        BigInteger txGasLimit = tx.getGasLimitAsInteger();
        Coin maximumPrice = tx.getGasPrice().multiply(txGasLimit);
        if (validationArgs.getAccountState().getBalance().compareTo(maximumPrice) >= 0
                || claimTransactionValidator.isClaimTxAndValid(tx, validationArgs.getRepositorySnapshot(), validationArgs.getActivationConfig())) {
            return TransactionValidationResult.ok();
        }

        return TransactionValidationResult.withError("insufficient funds");
    }
}
