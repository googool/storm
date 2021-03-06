package start.case4Window.tumbling;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseWindowedBolt;
import start.case4Window.AmtSpout;

public class Demo {
    public static void main(String[] args) {
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("spout", new AmtSpout());
        builder.setBolt("tumblingBolt",
                new TumblingBolt().withTumblingWindow(
                        new BaseWindowedBolt.Count(3)))
                .shuffleGrouping("spout");

        Config config = new Config();
        config.setDebug(false);

        try {
            if (args != null && args.length > 0) {
                config.setNumWorkers(3);
                StormSubmitter.submitTopology(args[0], config, builder.createTopology());
            } else {
                LocalCluster cluster = new LocalCluster();
                cluster.submitTopology("tumblingWindow", config, builder.createTopology());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
