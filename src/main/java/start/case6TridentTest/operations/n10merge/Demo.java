package start.case6TridentTest.operations.n10merge;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.trident.Stream;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.testing.FixedBatchSpout;
import org.apache.storm.tuple.Fields;
import start.case6TridentTest.operations.Datas;
import start.case6TridentTest.operations.n01filter.Filter;
import start.case6TridentTest.operations.n02function.Function;
import start.case6TridentTest.operations.n05aggregate.Aggregate;

public class Demo {

    public static StormTopology buildTopology() {

        FixedBatchSpout spout = new Datas().getSpout();
        spout.setCycle(true);

        TridentTopology topology = new TridentTopology();
        Stream stream = topology.newStream("spout", spout)
                .shuffle()
                .parallelismHint(4)// 设置并行度为 4
                .each(new Fields("date", "amt", "city", "product"), new Function.OperFunction(), new Fields("_date"))// 此时的Tuple有"date", "amt", "city", "product", "_date"5列数据
                .project(new Fields("_date", "amt"))// 投影操作：只保留 _date,amt 两个字段的数据，相当于select选取字段
                ;
        Stream stream1 = stream
                .groupBy(new Fields("_date"))
                .aggregate(new Fields("_date", "amt"), new Aggregate.CombinerAggre(), new Fields("amt_sum"));
        Stream stream2 = stream
                .groupBy(new Fields("_date"))
                .aggregate(new Fields("_date", "amt"), new Aggregate.CombinerAggreCount(), new Fields("amt_sum"));

        topology.merge(stream1,stream2)
                .each(new Fields("_date", "amt_sum"), new Filter.PrintFilter());
        return topology.build();
    }

    public static void main(String[] args) {
        Config conf = new Config();
        conf.setMaxSpoutPending(20);
        conf.setDebug(false);
        if (args.length == 0) {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("wordCounter", conf, buildTopology());
        }
    }
}
