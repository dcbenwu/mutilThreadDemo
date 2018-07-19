package mutilthreaddemo

class ScanJob {
    def scanService
    static triggers = {
        //simple repeatInterval: 5000l * 10000 // execute job once in 5 seconds
        cron name: 'ScanJob', cronExpression: "0 30 0 * * ? *" // seconds, minutes, hours,
    }

    def execute() {
        // execute job
        log.info("Scan job started at " + new Date())
        scanService.go()
        //scanService.testDb()
        /*new BooksEqpt(sn: "test", pn: "test", type: "test", location: "test",
                ip: "test",
                search_date: new Date(),
                device_type: "",
                state: ""
        ).save()*/

        //scanService.testInnerClass()
    }
}
