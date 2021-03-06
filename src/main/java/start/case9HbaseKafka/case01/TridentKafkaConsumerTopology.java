/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package start.case9HbaseKafka.case01;

import org.apache.storm.LocalDRPC;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.hbase.trident.mapper.SimpleTridentHBaseMapMapper;
import org.apache.storm.hbase.trident.state.HBaseMapState;
import org.apache.storm.trident.Stream;
import org.apache.storm.trident.TridentState;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.operation.BaseFilter;
import org.apache.storm.trident.operation.builtin.Count;
import org.apache.storm.trident.operation.builtin.Debug;
import org.apache.storm.trident.spout.ITridentDataSource;
import org.apache.storm.trident.testing.Split;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TridentKafkaConsumerTopology {
    protected static final Logger LOG = LoggerFactory.getLogger(TridentKafkaConsumerTopology.class);


    public static StormTopology newTopology(ITridentDataSource tridentSpout) {
        return newTopology(null, tridentSpout);
    }


    public static StormTopology newTopology(LocalDRPC drpc, ITridentDataSource tridentSpout) {

        final TridentTopology tridentTopology = new TridentTopology();
        addTridentState(tridentTopology, tridentSpout);
        return tridentTopology.build();
    }

    private static void addTridentState(TridentTopology tridentTopology, ITridentDataSource tridentSpout) {
        final Stream spoutStream = tridentTopology.newStream("spout1", tridentSpout).parallelismHint(2);

        HBaseMapState.Options options = new HBaseMapState.Options();
        options.tableName = "storm";
        options.columnFamily = "info";
        options.mapMapper = new SimpleTridentHBaseMapMapper("count");

        TridentState tridentState =
                spoutStream.each(spoutStream.getOutputFields(), new Debug(true))
                        .each(new Fields("str"), new Split(), new Fields("word"))
                        .groupBy(new Fields("word"))
                        .persistentAggregate(HBaseMapState.transactional(options), new Count(), new Fields("count"));

        tridentState.newValuesStream()
                .each(new Fields("word", "count"), new BaseFilter() {
                    @Override
                    public boolean isKeep(TridentTuple tuple) {
                        System.out.println("result >>>>  " + tuple);
                        return false;
                    }
                });
    }

}
