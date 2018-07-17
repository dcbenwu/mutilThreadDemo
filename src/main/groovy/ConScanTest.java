import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConScanTest {
    public static void main(String[] args) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(12, 30, 200, TimeUnit.MICROSECONDS,
                new ArrayBlockingQueue<Runnable>(30));

        ArrayList<String> arrayList = new ArrayList();
        /*//172.23.130.111,112,114
        for (int i=111; i<111+12; i++) {
            arrayList.add("172.23.130." + i);
        }

        // 172.23.130.147-159
        for (int i=147; i<147+8; i++) {
            arrayList.add("172.23.130." + i);
        }*/
        arrayList.add("172.23.130.111");
        //arrayList.add("172.23.130.112");
        arrayList.add("172.23.130.114");
        arrayList.add("172.23.130.147");
        arrayList.add("172.23.130.148");
        arrayList.add("172.23.130.149");
        //arrayList.add("172.23.130.150");
        arrayList.add("172.23.130.156");
        arrayList.add("172.23.130.157");
        arrayList.add("172.23.130.158");
        arrayList.add("172.23.130.159");
        arrayList.add("172.23.130.164");
        arrayList.add("172.23.130.165");
        arrayList.add("172.23.130.166");
        arrayList.add("172.23.130.167");
        //arrayList.add("172.23.130.169");
        arrayList.add("172.23.130.190");
        arrayList.add("172.23.130.191");
        arrayList.add("172.23.130.192");
        arrayList.add("172.23.130.193");
        arrayList.add("172.23.130.195");



        for(String ip: arrayList) {
            ScanTask scanTask = new ScanTask(ip);
            executor.execute(scanTask);
            System.out.println("线程池中线程数目："+executor.getPoolSize()+"，队列中等待执行的任务数目："+
                    executor.getQueue().size()+"，已执行玩别的任务数目："+executor.getCompletedTaskCount());
        }
        executor.shutdown();
    }
}

class ScanTask implements Runnable {

    static final int ds_port = 3083;
    static final int timeout = 3*1000;
    private String ip;
    TestTelnet testTelnet;
    public ScanTask(String ip) {
        this.ip = ip;
        try {
            testTelnet = new TestTelnet(ip, ds_port, timeout);
        } catch (Exception e) {
            testTelnet = null;
            //e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (testTelnet == null) {
            System.out.println("Telnet Client initialing failed on ip " + ip);
            return;
        }
        System.out.println("scanOne " + ip);
        String reStr = testTelnet.sendCommand("act-user::Admin1:123::*;", '>');
        //System.out.println(reStr.replaceFirst("act-user::Admin1:123::*;", ""));

        String reStr2 = testTelnet.sendCommand("act-user::Admin1:123::1Transport!;", '>');
        //System.out.println(reStr2.replaceFirst("act-user::Admin1:123::1Transport!;", ""));
        String reStr3 = testTelnet.sendCommand("rtrv-inv::all:100:::;", '>');
        //System.out.println(reStr3.replaceFirst("rtrv-inv::all:100:::;", ""));
        testTelnet.disconnect();
        System.out.println("end of scanOne " + ip);
    }
}
