/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cjx.com.trident.kfkTridentHBase;

import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.security.UserProvider;
import org.apache.storm.hbase.bolt.mapper.HBaseProjectionCriteria;
import org.apache.storm.hbase.common.ColumnList;
import org.apache.storm.hbase.common.Utils;
import org.apache.storm.hbase.security.HBaseSecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class KfkHBaseClient implements Closeable{
    private static final Logger LOG = LoggerFactory.getLogger(KfkHBaseClient.class);

    private HTable table;

    public KfkHBaseClient(Map<String, Object> map , final Configuration configuration, final String tableName) {
        try {
            UserProvider provider = HBaseSecurityUtil.login(map, configuration);
            this.table = Utils.getTable(provider, configuration, tableName);
        } catch(Exception e) {
            throw new RuntimeException("HBase bolt preparation failed: " + e.getMessage(), e);
        }
    }

    public List<Mutation> constructMutationReq(byte[] rowKey, ColumnList cols, Durability durability) {
        List<Mutation> mutations = Lists.newArrayList();

        if (cols.hasColumns()) {
            Put put = new Put(rowKey);
            put.setDurability(durability);
            for (ColumnList.Column col : cols.getColumns()) {
                if (col.getTs() > 0) {
                    put.add(
                            col.getFamily(),
                            col.getQualifier(),
                            col.getTs(),
                            col.getValue()
                    );
                } else {
                    put.add(
                            col.getFamily(),
                            col.getQualifier(),
                            col.getValue()
                    );
                }
            }
            mutations.add(put);
        }

        if (cols.hasCounters()) {
            Increment inc = new Increment(rowKey);
            inc.setDurability(durability);
            for (ColumnList.Counter cnt : cols.getCounters()) {
                inc.addColumn(
                        cnt.getFamily(),
                        cnt.getQualifier(),
                        cnt.getIncrement()
                );
            }
            mutations.add(inc);
        }

        if (cols.hasColumnsToDelete()) {
            Delete delete = new Delete(rowKey);
            delete.setDurability(durability);
            for (ColumnList.Column col : cols.getColumnsToDelete()) {
                if (col.getTs() > 0) {
                    delete.addColumn(col.getFamily(), col.getQualifier(), col.getTs());
                } else {
                    delete.addColumn(col.getFamily(), col.getQualifier());
                }
            }
            mutations.add(delete);
        }

        if (mutations.isEmpty()) {
            mutations.add(new Put(rowKey));
        }
        return mutations;
    }

    public void batchMutate(List<Mutation> mutations) throws Exception {
        Object[] result = new Object[mutations.size()];
        try {
            table.batch(mutations, result);
        } catch (InterruptedException e) {
            LOG.warn("Error performing a mutation to HBase.", e);
            throw e;
        } catch (IOException e) {
            LOG.warn("Error performing a mutation to HBase.", e);
            throw e;
        }
    }


    public Get constructGetRequests(byte[] rowKey, HBaseProjectionCriteria projectionCriteria) {
        Get get = new Get(rowKey);

        if (projectionCriteria != null) {
            for (byte[] columnFamily : projectionCriteria.getColumnFamilies()) {
                get.addFamily(columnFamily);
            }

            for (HBaseProjectionCriteria.ColumnMetaData columnMetaData : projectionCriteria.getColumns()) {
                get.addColumn(columnMetaData.getColumnFamily(), columnMetaData.getQualifier());
            }
        }

        return get;
    }

    public Result[] batchGet(List<Get> gets) throws Exception {
        try {
            return table.get(gets);
        } catch (Exception e) {
            LOG.warn("Could not perform HBASE lookup.", e);
            throw e;
        }
    }

    public ResultScanner scan(byte[] startRow, byte[] stopRow) throws Exception {
        try {
            if (startRow == null) {
                startRow = HConstants.EMPTY_START_ROW;
            }
            if (stopRow == null) {
                stopRow = HConstants.EMPTY_END_ROW;
            }

            Scan scan = new Scan(startRow, stopRow);
            return table.getScanner(scan);
        } catch (Exception e) {
            LOG.warn("Could not open HBASE scanner.", e);
            throw e;
        }
    }


    public ResultScanner scanLike(String rowKeyLike) throws Exception {
        try {

            PrefixFilter filter = new PrefixFilter(rowKeyLike.getBytes());
            Scan scan = new Scan();
            scan.setFilter(filter);
            ResultScanner scanner = table.getScanner(scan);

            return scanner;
        } catch (Exception e) {
            LOG.warn("Could not open HBASE scanner.", e);
            throw e;
        }
    }


    public boolean exists(Get get) throws Exception {
        try {
            return table.exists(get);
        } catch (Exception e) {
            LOG.warn("Could not perform HBASE existence check.", e);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        table.close();
    }
}
