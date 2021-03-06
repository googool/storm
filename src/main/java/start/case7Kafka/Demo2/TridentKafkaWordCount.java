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
package start.case7Kafka.Demo2;


import org.apache.storm.Config;
import org.apache.storm.kafka.StringScheme;
import org.apache.storm.kafka.ZkHosts;
import org.apache.storm.kafka.trident.TransactionalTridentKafkaSpout;
import org.apache.storm.kafka.trident.TridentKafkaConfig;
import org.apache.storm.spout.SchemeAsMultiScheme;

import java.io.Serializable;

public class TridentKafkaWordCount implements Serializable {

    public static void main(String[] args) throws Exception {
        final String[] zkBrokerUrl = parseUrl(args);
        final String topicName = "test1";
        Config tpConf = LocalSubmitter.defaultConfig();
        final LocalSubmitter localSubmitter = LocalSubmitter.newInstance();
        final String prodTpName = "kafkaBolt";
        final String consTpName = "wordCounter";
        try {
            // Producer
            //localSubmitter.submit(prodTpName, tpConf, KafkaProducerTopology.newTridentTopology(zkBrokerUrl[1], topicName));
            // Consumer
            localSubmitter.submit(consTpName, tpConf, TridentKafkaConsumerTopology.newTopology(localSubmitter.getDrpc(),
                    new TransactionalTridentKafkaSpout(newTridentKafkaConfig(zkBrokerUrl[0]))));
        } finally {
            // kill
//                localSubmitter.kill(prodTpName);
//                localSubmitter.kill(consTpName);
//                // shutdown
//                localSubmitter.shutdown();
        }

    }

    private static String[] parseUrl(String[] args) {
        String zkUrl = "bigdata-pro01:2181,bigdata-pro02:2181,bigdata-pro03:2181";        // the defaults.
        String brokerUrl = "bigdata-pro01:9092,bigdata-pro02:9092,bigdata-pro03:9092";

        System.out.println("Using Kafka zookeeper uHrl: " + zkUrl + " broker url: " + brokerUrl);
        return new String[]{zkUrl, brokerUrl};
    }

    public static TridentKafkaConfig newTridentKafkaConfig(String zkUrl) {
        ZkHosts hosts = new ZkHosts(zkUrl);
        TridentKafkaConfig config = new TridentKafkaConfig(hosts, "test");
        config.scheme = new SchemeAsMultiScheme(new StringScheme());

        // Consume new data from the topic
        config.startOffsetTime = kafka.api.OffsetRequest.LatestTime();
        return config;
    }
}
