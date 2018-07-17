import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class BenScanTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Begin at " + new Date())
        def begin = System.currentTimeMillis()
        ThreadPoolExecutor executor = new ThreadPoolExecutor(12, 30, 200, TimeUnit.MICROSECONDS,
                new ArrayBlockingQueue<Runnable>(30));

        def subNetList = [96, 97, 99, 151, 153, 155, 160, 157, 100, 201, 202]
        subNetList.each {
            System.out.println("new task for sub net 3: 172.29." +it.intValue() )
            BenScanTask benScanTask = new BenScanTask(it.intValue());
            executor.execute(benScanTask);
            System.out.println("线程池中线程数目：" + executor.getPoolSize() + "，队列中等待执行的任务数目：" +
                    executor.getQueue().size() + "，已执行玩别的任务数目：" + executor.getCompletedTaskCount());
        }

        executor.shutdown();

        def end = System.currentTimeMillis()
        System.out.println("Total time estimate " + (end - begin)/1000/60 + " minutes")
    }
}


class BenScanTask implements Runnable {

    static final int ds_port = 3083;
    static final int timeout = 3*1000;

    TestTelnet testTelnet = null;

    private int sub3Index;
    public BenScanTask(int subIndex) {
        this.sub3Index = subIndex;
    }

    @Override
    public void run() {
        def ip = ""
        (2..254).each {
            ip = "172.29."+sub3Index+"."+it
            scan(ip)
            if (it == 254) {
                System.out.println(ip + " finished at " + new Date())
            }
        }
    }

    private void scan(String ip) {
        //System.out.println("try to scan on ip " + ip);

        try {
            testTelnet = new TestTelnet(ip, ds_port, timeout);
        } catch (Exception e) {
            //System.out.println("Telnet Client initialing failed on ip " + ip);
            //System.out.println(testTelnet)
            return
        }

        /*try {
            testTelnet.sendCommand("act-user::Admin1:123::*;", '>');
        } catch (MissingMethodException e1) {
            e1.printStackTrace()
        }*/
        String reStr = testTelnet.sendCommand("act-user::Admin1:123::*;", '>');
        //System.out.println(reStr.replaceFirst("act-user::Admin1:123::*;", ""));

        String reStr2 = testTelnet.sendCommand("act-user::Admin1:123::1Transport!;", '>');
        //System.out.println(reStr2.replaceFirst("act-user::Admin1:123::1Transport!;", ""));
        String reStr3 = testTelnet.sendCommand("rtrv-inv::all:100:::;", '>');
        //System.out.println(reStr3.replaceFirst("rtrv-inv::all:100:::;", ""));
        testTelnet.disconnect();
        //System.out.println("end of scan on ip " + ip);
    }
}