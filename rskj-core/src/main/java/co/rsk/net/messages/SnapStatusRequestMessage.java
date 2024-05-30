/*
 * This file is part of RskJ
 * Copyright (C) 2023 RSK Labs Ltd.
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

package co.rsk.net.messages;

import org.ethereum.util.RLP;

public class SnapStatusRequestMessage extends Message {

    public SnapStatusRequestMessage() {
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.SNAP_STATUS_REQUEST_MESSAGE;
    }

    @Override
    public byte[] getEncodedMessage() {
        return RLP.encodedEmptyList();
    }

    @Override
    public void accept(MessageVisitor v) {
        v.apply(this);
    }
}
