package start.case6TridentTest.operations.n01filter;

import org.apache.storm.trident.operation.BaseFilter;
import org.apache.storm.trident.operation.TridentOperationContext;
import org.apache.storm.trident.tuple.TridentTuple;

import java.util.Map;

public class Filter {
    public static class BiggerFilter extends BaseFilter {
        int filter;

        public BiggerFilter(int filter) {
            this.filter = filter;
        }

        @Override
        public boolean isKeep(TridentTuple tuple) {
            int _amt = tuple.getIntegerByField("amt");
            return _amt >= filter;
        }
    }

    public static class SmallerFilter extends BaseFilter {
        int filter;

        public SmallerFilter(int filter) {
            this.filter = filter;
        }

        @Override
        public boolean isKeep(TridentTuple tuple) {
            int _amt = tuple.getIntegerByField("amt");
            return _amt <= filter;
        }
    }

    public static class StringFilter extends BaseFilter {
        String[] filter;

        public StringFilter(String[] filter) {
            this.filter = filter;
        }

        @Override
        public boolean isKeep(TridentTuple tuple) {
            String word = tuple.getStringByField("word");
            for (String s : filter) {
                if (s.contains(word)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class PrintFilter extends BaseFilter {
        int numPar;

        @Override
        public void prepare(Map conf, TridentOperationContext context) {
            numPar = context.numPartitions();
            super.prepare(conf, context);
        }

        @Override
        public boolean isKeep(TridentTuple tuple) {
            System.out.println("result >>> " + tuple + "\tnumPar => " + numPar);
            return false;

        }
    }
}
