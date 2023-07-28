/*
 * This file is part of RskJ
 * Copyright (C) 2023 RSK Labs Ltd.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
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
package org.ethereum.rpc.validation;

import org.ethereum.rpc.exception.RskJsonRpcRequestException;
import org.junit.jupiter.api.Test;

import static org.ethereum.TestUtils.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

class HexValueValidatorTest {

    @Test
    void testValidHexadecimals() {
        assertDoesNotThrow(() -> HexValueValidator.isValid("0x0"));
        assertDoesNotThrow(() -> HexValueValidator.isValid("0x123"));
        assertDoesNotThrow(() -> HexValueValidator.isValid("0xabcdef"));
        assertDoesNotThrow(() -> HexValueValidator.isValid("0x0000000000000000000000000000000001000008"));
        assertDoesNotThrow(() -> HexValueValidator.isValid("0xABCDEF")); // Uppercase are not allowed in Ethereum
        assertDoesNotThrow(() -> HexValueValidator.isValid("0x0123456789")); //Numbers staring by 0x0 is not allowed in Ethereum
    }

    @Test
    void testInvalidHexadecimals() {
        assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid("0x"));
        assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid("0xGHIJKLMNOPQRSTUVWXYZ"));
        assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid("123456"));
        assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid("abcdef"));
        assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid("invalid"));
        assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid("0x1234g6"));
    }

    @Test
    void testValidHexadecimalWithoutPrefix() {
        assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid("123456"));
    }

    @Test
    void testInvalidHexadecimalWithPrefix() {
        assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid("0x1234g6"));
    }

    @Test
    void testWhitespaceString() {
        assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid(" "));
    }

    @Test
    void testEmptyString() {
        assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid(""));
    }

    @Test
    void testNullString() {
        assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid(null));
    }

    @Test
    void testValidErrorCodeOnException() {
        RskJsonRpcRequestException exception = assertThrows(RskJsonRpcRequestException.class, () -> HexValueValidator.isValid("123456"));
        assertEquals(-32602, exception.getCode());
    }
}