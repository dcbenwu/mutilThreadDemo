package mutilthreaddemo

import com.ben.BenTelnet
import groovy.sql.Sql
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.Semaphore

class TelnetTaskService implements Runnable{

    def dataSource

    public long startTimeMills = 0;
    private String ip;
    static final int ds_port = 3083;
    static final int timeout = 3*1000;
    private Date searchDate;

    Semaphore telnetLimit

    BenTelnet benTelnet = null;

    Logger log = LoggerFactory.getLogger(TelnetTaskService.class.name)

    public TelnetTaskService(ip, telnetLimit, searchDate) {
        this.ip = ip
        this.telnetLimit = telnetLimit
        this.searchDate = searchDate
    }

    public void testSql() {
        def db = new Sql(dataSource)
        def sql = "insert into books_eqpt (version,sn,pn,type,location,ip,search_date) values (1,'test','test','test','test','test','2010-07-13')"
        db.execute(sql)
    }

    @Override
    public void run() {

        String invStr = retrieveInventory()

        if (invStr != null) {
            matchDeviceInfo(invStr)
        }
    }

    private String retrieveInventory() {

        String reStr;

        telnetLimit.acquire()
        startTimeMills = System.currentTimeMillis()

        try {
            log.info("try to connect ip " + ip)
            benTelnet = new BenTelnet(ip, ds_port, timeout);
            log.info("try to send command to ip " + ip)

            reStr = benTelnet.sendCommand("act-user::Admin1:123::*;", ';');
            //log.info(reStr.replaceFirst("act-user::Admin1:123::*;", ""));
            Thread.sleep(100);
            log.info("try to send second command to ip " + ip)
            reStr = benTelnet.sendCommand("act-user::Admin1:123::1Transport!;", ';');
            Thread.sleep(100);
            //log.info(reStr.replaceFirst("act-user::Admin1:123::1Transport!;", ""));
            log.info("try to send third command to ip " + ip)
            reStr = benTelnet.sendCommand("rtrv-inv::all:100:::;", ';');
            //log.info(reStr.replaceFirst("rtrv-inv::all:100:::;", ""));
            benTelnet.disconnect();

            log.info("end of scan on ip " + ip);
        } catch (SocketException se) {
            log.info("Telnet Client initialing failed on ip " + ip)
            //se.printStackTrace()
        } catch (Exception e) {
            log.info("Telnet Client initialing failed on ip " + ip)
            //e.printStackTrace()
        }

        telnetLimit.release()

        return reStr
    }

    def matchDeviceInfo(inStr){

        def ignoreTypeList =['PORT_SHELF','HDPCT','PICO_SHELF','OPTICAL_SHELF','BFM']
        String invString = inStr
        invString = invString.replace('\\','').replace('"','')
        def lineList = invString.findAll('.+?::TYPE=.+?,SN=.+?,PN=.+?,')
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
            if (sn != "N/A" && ! (type in ignoreTypeList) ){
                log.info("find device sn " + sn + " ip" + ip)
                /*def device = new BooksEqpt(sn: sn, pn: pn, type: type, location: location,
                        ip: ip,
                        search_date: searchDate,
                        device_type: "",
                        state: ""
                )
                device.save(flush: true)*/
                def db = new Sql(dataSource)
                def sql = "insert into books_eqpt (version,sn,pn,type,location,ip,search_date) " +
                        "values (0," +
                        sn + "," +
                        pn + "," +
                        type + "," +
                        location + "," +
                        ip + "," +
                        searchDate.toString() +
                        ")"
                db.execute(sql)
                log.info("save one device sn: " + sn)
            }

        }
    }

}