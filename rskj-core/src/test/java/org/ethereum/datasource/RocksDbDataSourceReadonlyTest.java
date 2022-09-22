/*
 * This file is part of RskJ
 * Copyright (C) 2022 RSK Labs Ltd.
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

package org.ethereum.datasource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.ethereum.TestUtils.randomBytes;

@RunWith(MockitoJUnitRunner.class)
public class RocksDbDataSourceReadonlyTest {

    @Mock
    private static RocksDB dbMock;

    private RocksDbReadonlyStub dataSource;

    @Rule
    public TemporaryFolder databaseDir = new TemporaryFolder();

    private String dbPath;

    private String dbName;

    @Before
    public void setUp() throws Exception {
        File dbFolder = databaseDir.newFolder();

        dbPath = dbFolder.getPath();
        dbName = "test";

        dataSource = RocksDbReadonlyStub.create(dbName, dbPath);
        dataSource.init();
    }

    @Test
    public void putThrows() {
        byte[] key = randomBytes(32);
        byte[] value = randomBytes(32);

        Assert.assertThrows(ReadonlyDbDataSource.ReadOnlyException.class, () -> dataSource.put(key, value));
    }

    @Test
    public void deleteThrows() {
        byte[] key = randomBytes(32);

        Assert.assertThrows(ReadonlyDbDataSource.ReadOnlyException.class, () -> dataSource.delete(key));
    }

    @Test
    public void updateBatchThrows() {
        Assert.assertThrows(ReadonlyDbDataSource.ReadOnlyException.class, () -> dataSource.updateBatch(Collections.emptyMap(), Collections.emptySet()));
    }

    @Test
    public void directoriesNotCreated() {
        Assert.assertFalse(Files.exists(Paths.get(dbPath, dbName)));
    }

    @Test
    public void optionCreateIfMissingFalse() {
        Assert.assertFalse(dataSource.optionCreateIfMissing);
    }

    private static class RocksDbReadonlyStub extends RocksDbDataSourceReadonly {

        private Boolean optionCreateIfMissing;

        public static RocksDbReadonlyStub create(String name, String databaseDir) {
            return new RocksDbReadonlyStub(name, databaseDir);
        }

        protected RocksDbReadonlyStub(String name, String databaseDir) {
            super(name, databaseDir);
        }

        @Override
        protected RocksDB openDb(Options options, Path dbPath) {
            return dbMock;
        }

        @Override
        public void customiseOptions(Options options) {
            super.customiseOptions(options);
            optionCreateIfMissing = options.createIfMissing();
        }

    }
}