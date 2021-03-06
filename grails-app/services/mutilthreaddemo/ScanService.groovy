package mutilthreaddemo

import com.ben.BenTelnet
import com.ben.TelnetTask
import grails.transaction.Transactional
import groovy.sql.Sql
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

//@Transactional
class ScanService {

    def dataSource
    private Date searchDate = null

    static AtomicInteger atomicSerial = new AtomicInteger(0);

    static int serial = 0

    def scanStartTime
    def scanEndTime

    synchronized static void setSerial() {
        serial ++
    }

    static void resetSerial() {
        serial = 0
    }

    def setSearchDate(Date date) {
        searchDate = date
    }

    def getSearchDate() {
        searchDate
    }

    def serviceMethod() {

        log.info()
    }

    def testManualSql() {
        Thread.start {
            def db = new Sql(dataSource)
            def sql = "insert into books_eqpt (version,sn,pn,type,location,ip,search_date) values (1,'test','test','test','test','test','2010-07-13')"
            db.execute(sql)
        }
    }
    def testManualSql2() {
        TelnetTaskService telnetTask = new TelnetTaskService(null,null,null)
        telnetTask.testSql()
    }

    def testDb() {
        Thread.start {
            new BooksEqpt(sn: "test", pn: "test", type: "test", location: "test",
                    ip: "test",
                    search_date: new Date(),
                    device_type: "",
                    state: ""
            ).save()


        }

    }

    def testInnerClass() {
        new BenThread().start()
    }

    private class BenThread extends Thread {
        public BenThread(){}
        public void run(){
            /*new BooksEqpt(sn: "test", pn: "test", type: "test", location: "test",
                    ip: "test",
                    search_date: new Date(),
                    device_type: "",
                    state: ""
            ).save()*/
            def db = new Sql(dataSource)
            def sql = "insert into books_eqpt (version,sn,pn,type,location,ip,search_date) values (1,'test','test','test','test','test','2010-07-13')"
            db.execute(sql)
        }
    }
    
    def go() {

        resetSerial()

        def begin = new Date()
        scanStartTime = begin
        log.info("Begin at " + begin)

        // set search date for every one time search
        def dateStr = begin.format("yyyy-MM-dd")
        def searchDate = Date.parse("yyyy-MM-dd", dateStr)
        setSearchDate(searchDate)

        def ipArr = []
        def ip
        def subNetList = [96, 97, 99, 151, 153, 155, 157, 100, 201, 202, 178]
        //def subNetList = [160]//[96, 97, 99, 151, 153, 155, 160, 157, 100, 201, 202]

        subNetList.each { net3 ->
            (2..254).each { net4 ->
                ip = "172.29."+net3+"."+net4
                ipArr.add(ip)
                //ipArr.push(ip)
            }
        }

        (160).each { net3 ->
            (101..254).each { net4 ->
                ip = "172.29."+net3+"."+net4
                ipArr.add(ip)
            }
        }

        (130).each { net3 ->
            (2..254).each { net4 ->
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
            log.info("Summit task " + (++ii) + " for ip " + it)
            TelnetTaskInner oneScanTask = new TelnetTaskInner(it, telnetLimits, searchDate)
            Future future = executorService.submit(oneScanTask)
            futureMap[it] = future
            // 起一个线程在后面检查所有的future list，超过时间的给他cancel掉就完美
            log.info("Pool size " + executorService.toString())
        }

        Thread.start {
            Thread.sleep(1000*60)
            while (true) {
                Thread.sleep(10)
                if (futureMap.size() == 0) {
                    log.info("All scan threads are terminated. End record the timestamp.")
                    BooksScanTime.withNewSession {
                        def now = new Date()
                        def dateString = now.format("yyyy-MM-dd")
                        def curDate = Date.parse("yyyy-MM-dd", dateString)
                        new BooksScanTime(scanStartTime: scanStartTime, scanEndTime: new Date(), curDate: curDate).save()
                    }
                    log.info("---End of scan process.")
                    break
                }
            }
        }

        Thread.start {
            def futureMapTemp = futureMap.clone()
            ExecutorService es = executorService
            Semaphore semaphore = telnetLimits

            def monitorStartTime = System.currentTimeMillis()

            Thread.sleep(1000)
            while (futureMap.size() > 0) {
                Thread.sleep(200)
                def currentTime = System.currentTimeMillis()
                def delta = (currentTime - monitorStartTime)/(1000)
                // 5min 300sec
                if (delta > 300) {
                    log.info("-------Debug info--------")
                    Iterator iterator = futureMap.iterator()
                    while (iterator.hasNext()) {
                        def one = iterator.next()
                        FutureTask futureTask = one.value
                        if (futureTask.isDone()) {
                            iterator.remove()
                            log.info("remove one " + one.key)
                        } else {

                            iterator.remove()
                            log.info("  Cancel Telnet Task,  IP: " + one.key)
                            /*TelnetTaskInner telnetTaskInner = futureTask.get()
                            def timeOut = System.currentTimeMillis() - telnetTaskInner.startTimeMills
                            log.info("Telnet task estimate " + timeOut + " ms")*/
                            futureTask.cancel(true)

                        }
                    }
                    scanEndTime = new Date()
                    log.info("-------------------------")

                } else {
                    Iterator iterator = futureMap.iterator()
                    while (iterator.hasNext()) {
                        def one = iterator.next()
                        FutureTask futureTask = one.value
                        if (futureTask.isDone()) {
                            iterator.remove()
                            log.info("remove one " + one.key)
                        }
                    }
                    log.info("Remain " + futureMap.size())
                }
            }

            /*while (futureMap.size() > 0) {
                Thread.sleep(1000)
                futureMap.iterator().each {
                    FutureTask futureTask = it.value
                    if(futureTask.isDone()) {
                        it.remove()
                        log.info("remove one " + it.key)
                    }
                    //OneScanTask task = futureTask.get()
                }
                log.info("Remain " + futureMap.size())
            }*/

        }

        executorService.shutdown()
        log.info("acquireUninterruptibly Semaphore")
        telnetLimits.acquireUninterruptibly(permits)
        log.info("release Semaphore")
        telnetLimits.release(permits)
        scanEndTime = new Date()
        log.info("Task start at " + begin + " Semaphore released   at " + new Date())
    }

    private class TelnetTaskInner implements Runnable{

        def dataSource

        public long startTimeMills = 0;
        private String ip;
        static final int ds_port = 3083;
        static final int timeout = 3*1000;
        private Date searchDate;

        private final String rtrvTag = "tag456";
        private final String rtrvTagVerify = "M  " + rtrvTag + " COMPLD";
        private final String retrieveInventoryCmd = "rtrv-inv::all:" + rtrvTag + ":::;";


        Semaphore telnetLimit

        BenTelnet benTelnet = null;

        Logger log = LoggerFactory.getLogger(TelnetTaskInner.class.name)

        public TelnetTaskInner(ip, telnetLimit, searchDate) {
            this.ip = ip
            this.telnetLimit = telnetLimit
            this.searchDate = searchDate
            log.info("TelnetTask created on ip " + ip)
        }

        public void testSql() {
            def db = new Sql(dataSource)
            def sql = "insert into books_eqpt (version,sn,pn,type,location,ip,search_date) values (1,'test','test','test','test','test','2010-07-13')"
            db.execute(sql)
        }

        @Override
        public void run() {
            log.info("Telnet Thread for " + ip + " is triggered.")
            String invStr = retrieveInventory()

            if (invStr != null && invStr.contains(rtrvTagVerify)) {
                log.info("got rtrv inv resp on " + ip)
                if (invStr.contains("REPT EVT SESSION")){
                    abnormalIP("EVT")
                } else if (invStr.contains("REPT DBCHG")) {
                    abnormalIP("DBCHG")
                } else if (invStr.contains("REPT ALM")) {
                    abnormalIP("ALM")
                }
                newIp()
                log.info("try to find devices on ip " + ip)
                //log.info("Debug: invStr = " + invStr)
                matchDeviceInfo(invStr)
            } else {
                log.error("Warning: retrieveInventory " + ip + ". Content: " + invStr)
            }

            //telnetLimit.release()
        }

        private String retrieveInventory() {
            String reStr;
            try {
                log.info("try to retrieveInventory on ip " + ip)

                def semaphoreBegin = System.currentTimeMillis()
                telnetLimit.acquire()

                def semaphoreEnd = System.currentTimeMillis()
                log.info("Acquire semaphore for " + ip + " takes " + (semaphoreEnd - semaphoreBegin) + " ms")
                startTimeMills = System.currentTimeMillis()
                log.info("try to connect ip " + ip)
                benTelnet = new BenTelnet(ip, ds_port, timeout);

                log.info("try to login ip " + ip)
                def loginStr = benTelnet.sendCommand("act-user::Admin1:123::1Transport!;", ';');
                //log.info(loginStr.replaceFirst("act-user::Admin1:123::1Transport!;", ""));
                if ( loginStr != null && loginStr.contains("M  123 COMPLD")) {
                    Thread.sleep(50);
                    def inhStr = benTelnet.sendCommand("INH-MSG-ALL:::234;", ';')
                    if (inhStr != null && inhStr.contains( "M  234 COMPLD")) {
                        log.info("try to retrieve inventory from ip " + ip)
                        Thread.sleep(50);
                        reStr = benTelnet.sendCommand(retrieveInventoryCmd, ';');

                        log.info("retrieve inventory from ip " + ip + " : " + reStr);
                    } else {
                        log.warn(" INH-MSG-ALL command failed with resp " + inhStr)
                    }
                } else {
                    log.warn("telnet login failed with message: " + loginStr)
                }
                benTelnet.disconnect();
                log.info("end of talking to device " + ip);
            } catch (SocketException se) {
                log.info("Telnet Client initialing failed on ip " + ip)
                log.info("SocketException Message: " + se.getMessage())
                //se.printStackTrace()
                benTelnet.free()
            } catch (Exception e) {
                log.info("Telnet Client send command failed on ip " + ip)
                log.info("Other Exception Message: " + e.getMessage())
                //e.printStackTrace()
                benTelnet.free()
            } finally {
                log.info(" release semaphore for ip " + ip)
                telnetLimit.release()
            }

            return reStr
        }

        private void newIp() {
            log.info("Record ip with respond. Which is " + ip)
            BooksIp.withNewSession {
                def findRecord = BooksIp.findByIp(ip)
                if ( ! findRecord ) {
                    def ret
                    try {
                        ret = new BooksIp(ip: ip).save()
                    } catch (Exception e) {
                        log.error(e.getMessage())
                    }
                    if (! ret ) {
                        log.error("Error: save ip " + ip + " failed.")
                    }
                }
            }
        }

        private void abnormalIP(state) {
            log.info("Record ip with abnormal respond. Which is " + ip)
            BooksIp.withNewSession {
                def findRecord = BooksIp.findByIp(ip)
                try {
                    if (!findRecord) {
                        new BooksIp(ip: ip, state: state).save()
                    } else {
                        findRecord.state = state
                        findRecord.save()
                    }
                }catch (Exception e) {
                    log.error("Change IP state failed.")
                    log.error(e.getMessage())
                }
            }
        }

        def matchDeviceInfo(inStr){

            def ignoreTypeList =['PORT_SHELF','HDPCT','PICO_SHELF','OPTICAL_SHELF','BFM']
            String invString = inStr
            invString = invString.replace('\\','').replace('"','')
            def lineList = invString.findAll('.+?::TYPE=.+?,SN=.+?,PN=.+?,')
            if (lineList.size() > 0) {
                log.info("Record match devices on ip " + ip)
            } else {
                log.info("No records match devices on ip " + ip)
            }
            lineList.each {
                String line = it
                def matcher = line =~ '(.+?)::TYPE=(.+?),SN=(.+?),PN=(.+?),'
                String location = matcher[0][1]
                String type = matcher[0][2]
                String sn = matcher[0][3]
                String pn = matcher[0][4]
                location = location.trim();type = type.trim();sn = sn.trim();pn = pn.trim()
                log.info(location + " " + type + " " + sn + " " + pn)
                /*String searchDateStr = new Date().format("yyyy-MM-dd")
                Date searchDate = Date.parse("yyyy-MM-dd", searchDateStr)*/
                if ((sn != "N/A" && sn != "NA" && sn != "") && ! (type in ignoreTypeList) ){

                    setSerial()
                    def count = atomicSerial.incrementAndGet()

                    log.info("find device serial " + count + " sn " + sn + " ip " + ip)
                    BooksEqpt.withNewSession {
                        def device = new BooksEqpt(sn: sn, pn: pn, type: type, location: location,
                                serial: count,
                                ip: ip,
                                search_date: searchDate,
                                device_type: "",
                                state: ""
                        )
                        log.info("try to save object ip " + ip + ",sn " + sn + ",pn " + pn + ",location " + location)
                        def retObj = device.save(flush: true)
                        if (! retObj) {
                            log.error("device not saved with ip,sn " + ip + "," + sn + device.errors)
                        }
                    }
                    /*def db = new Sql(dataSource)
                    def sql = "insert into books_eqpt (version,sn,pn,type,location,ip,search_date) " +
                            "values (0," +
                            sn + "," +
                            pn + "," +
                            type + "," +
                            location + "," +
                            ip + "," +
                            searchDate.toString() +
                            ")"
                    db.execute(sql)*/
                    log.info("save one device sn: " + sn)
                }

            }
        }

    }
}
