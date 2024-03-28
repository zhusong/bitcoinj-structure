import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Uninterruptibles;
import org.bitcoin.core.AbstractExecutionThreadServiceImpl;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class ATest {

    private int a = 0;

    public static void print(){
        ATest aTest = new ATest();
        System.out.println(aTest.a);
    }
}
