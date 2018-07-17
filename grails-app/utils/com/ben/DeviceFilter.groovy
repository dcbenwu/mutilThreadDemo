package com.ben

class DeviceFilter {
    def matchDeviceInfo(inStr){

        def ignoreTypeList =['PORT_SHELF','HDPCT','PICO_SHELF','OPTICAL_SHELF','BFM']
        String invString = inStr
        invString = invString.replace('\\','').replace('"','')
        def lineList = invString.findAll('.+?::TYPE=.+?,SN=.+?,PN=.+?,')
        if (lineList.size() > 0) {
            println("Record match devices ")
        }
        lineList.each {
            String line = it
            def matcher = line =~ '(.+?)::TYPE=(.+?),SN=(.+?),PN=(.+?),'
            String location = matcher[0][1]
            String type = matcher[0][2]
            String sn = matcher[0][3]
            String pn = matcher[0][4]
            location = location.trim();type = type.trim();sn = sn.trim();pn = pn.trim()

            /*String searchDateStr = new Date().format("yyyy-MM-dd")
            Date searchDate = Date.parse("yyyy-MM-dd", searchDateStr)*/
            if ((sn != "N/A" && sn != "NA" && sn != "")){

                if (! (type in ignoreTypeList) ) {
                    System.out.println("sn: " + sn + ",pn: " + pn + ",type: " + type)
                } else {
                    System.out.println("---Warning Matches ignoreTypeList---")
                    System.out.println("sn: " + sn + ",pn: " + pn + ",type: " + type)
                }
            }

        }
    }
}
