import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ConcScanPool {

    public static void main(args) {

        def begin = new Date()
        System.out.println("Begin at " + begin)

        def ipArr = []
        def ip
        def subNetList = [96, 97, 99, 151, 153, 155, 160, 157, 100, 201, 202]
        //def subNetList = [160]//[96, 97, 99, 151, 153, 155, 160, 157, 100, 201, 202]
        (2..254).each { net4 ->
            subNetList.each { net3 ->
                ip = "172.29."+net3+"."+net4
                ipArr.add(ip)
                //ipArr.push(ip)
            }
        }
        (2..254).each { net4 ->
            (130).each { net3 ->
                ip = "172.23."+net3+"."+net4
                ipArr.add(ip)
            }
        }


        // Concurrent
        def futureMap = [:]
        ExecutorService executorService = Executors.newCachedThreadPool();
        final int permits = 100;
        Semaphore telnetLimits = new Semaphore(permits)
        int ii = 0
        ipArr.each {
            System.out.println("Summit task " + (++ii))
            OneScanTask oneScanTask = new OneScanTask(it, telnetLimits)
            Future future = executorService.submit(oneScanTask)
            futureMap[it] = future
            // 起一个线程在后面检查所有的future list，超过时间的给他cancel掉就完美
            System.out.println("Pool size " + executorService.toString())
        }

        Thread.start {
            def futureMapTemp = futureMap.clone()
            ExecutorService es = executorService
            Semaphore semaphore = telnetLimits

            while (futureMap.size() > 0) {
                Thread.sleep(1000)
                Iterator iterator = futureMap.iterator()
                while (iterator.hasNext()) {
                    def one = iterator.next()
                    FutureTask futureTask = one.value
                    if (futureTask.isDone()) {
                        iterator.remove()
                        System.out.println("remove one " + one.key)
                    }
                }
                System.out.println("Remain " + futureMap.size())
            }

            /*while (futureMap.size() > 0) {
                Thread.sleep(1000)
                futureMap.iterator().each {
                    FutureTask futureTask = it.value
                    if(futureTask.isDone()) {
                        it.remove()
                        System.out.println("remove one " + it.key)
                    }
                    //OneScanTask task = futureTask.get()
                }
                System.out.println("Remain " + futureMap.size())
            }*/

        }

        executorService.shutdown()
        System.err.println("acquireUninterruptibly Semaphore")
        telnetLimits.acquireUninterruptibly(permits)
        System.err.println("release Semaphore")
        telnetLimits.release(permits)
        System.out.println("Begin at " + begin + " \nEnd   at " + new Date())
    }

}


class OneScanTask implements Runnable{

    public long startTimeMills = 0;
    private String ip;
    static final int ds_port = 3083;
    static final int timeout = 3*1000;

    Semaphore telnetLimit

    TestTelnet testTelnet = null;

    public OneScanTask(ip,telnetLimit){
        this.ip = ip
        this.telnetLimit = telnetLimit
    }

    @Override
    void run() {
        scan(ip)
    }

    private void scan(ip) {

        /*if (telnetLimit.availablePermits() > 0) {
            System.out.println("have resources")
        } else {
            System.out.println("no resources")
        }*/

        telnetLimit.acquire()
        startTimeMills = System.currentTimeMillis()
        try {
            System.out.println("try to connect ip " + ip)
            testTelnet = new TestTelnet(ip, ds_port, timeout);
            System.out.println("try to send command to ip " + ip)
            def reStr;
            reStr = testTelnet.sendCommand("act-user::Admin1:123::*;", ';');
            //System.out.println(reStr.replaceFirst("act-user::Admin1:123::*;", ""));
            Thread.sleep(100);
            System.out.println("try to send second command to ip " + ip)
            reStr = testTelnet.sendCommand("act-user::Admin1:123::1Transport!;", ';');
            Thread.sleep(100);
            //System.out.println(reStr.replaceFirst("act-user::Admin1:123::1Transport!;", ""));
            System.out.println("try to send third command to ip " + ip)
            reStr = testTelnet.sendCommand("rtrv-inv::all:100:::;", ';');
            //System.out.println(reStr.replaceFirst("rtrv-inv::all:100:::;", ""));
            testTelnet.disconnect();

            System.out.println("end of scan on ip " + ip);
        } catch (SocketException se) {
            System.out.println("Telnet Client initialing failed on ip " + ip)
            //se.printStackTrace()
        } catch (Exception e) {
            System.out.println("Telnet Client initialing failed on ip " + ip)
            //e.printStackTrace()
        }

        telnetLimit.release()
    }
}