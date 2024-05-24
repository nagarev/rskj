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

package co.rsk.mine;

import co.rsk.core.Coin;
import co.rsk.mine.minGasPrice.MinGasPriceProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by mario on 22/12/16.
 */
class MinimumGasPriceCalculatorTest {

    @Test
    void increaseMgp() {
        long target = 2000L;
        Coin prev = Coin.valueOf(1000L);
        MinimumGasPriceCalculator mgpCalculator = new MinimumGasPriceCalculator(new MinGasPriceProvider(target));
        Coin mgp = mgpCalculator.calculate(prev);
        Assertions.assertEquals(Coin.valueOf(1010), mgp);
    }

    @Test
    void decreaseMGP() {
        Coin prev = Coin.valueOf(1000L);
        long target = 900L;
        MinimumGasPriceCalculator mgpCalculator = new MinimumGasPriceCalculator(new MinGasPriceProvider(target));
        Coin mgp = mgpCalculator.calculate(prev);
        Assertions.assertEquals(Coin.valueOf(990), mgp);
    }

    @Test
    void mgpOnRage() {
        Coin prev = Coin.valueOf(1000L);
        long target = 995L;
        MinimumGasPriceCalculator mgpCalculator = new MinimumGasPriceCalculator(new MinGasPriceProvider(target));
        Coin mgp = mgpCalculator.calculate(prev);
        Assertions.assertEquals(Coin.valueOf(target), mgp);
    }

    @Test
    void previousMgpEqualsTarget() {
        Coin prev = Coin.valueOf(1000L);
        long target = 1000L;
        MinimumGasPriceCalculator mgpCalculator = new MinimumGasPriceCalculator(new MinGasPriceProvider(target));
        Coin mgp = mgpCalculator.calculate(prev);
        Assertions.assertEquals(Coin.valueOf(target), mgp);
    }

    @Test
    void previousValueIsZero() {
        long target = 1000L;
        MinimumGasPriceCalculator mgpCalculator = new MinimumGasPriceCalculator(new MinGasPriceProvider(target));
        Coin mgp = mgpCalculator.calculate(Coin.ZERO);
        Assertions.assertEquals(Coin.valueOf(1L), mgp);
    }

    @Test
    void previousValueIsSmallTargetIsZero() {
        long target = 0L;
        MinimumGasPriceCalculator mgpCalculator = new MinimumGasPriceCalculator(new MinGasPriceProvider(target));
        Coin mgp = mgpCalculator.calculate(Coin.valueOf(1L));
        Assertions.assertEquals(Coin.ZERO, mgp);
    }

    @Test
    void cantGetMGPtoBeNegative() {
        Coin previous = Coin.ZERO;
        long target = -100L;
        MinimumGasPriceCalculator mgpCalculator = new MinimumGasPriceCalculator(new MinGasPriceProvider(target));
        previous = mgpCalculator.calculate(previous);
        Assertions.assertTrue(previous.compareTo(Coin.valueOf(-1)) > 0);
    }
}
