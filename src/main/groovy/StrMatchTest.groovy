import org.aspectj.weaver.ast.Not

class StrMatchTest {
    static void main(args) {
        TestTelnet telnet = new TestTelnet("172.23.130.158", 3083, 1000 * 3);
        String reStr = telnet.sendCommand("act-user::Admin1:123::*;", ';');
        //System.out.println(reStr.replaceFirst("act-user::Admin1:123::*;", ""));
        System.out.println(reStr);

        String reStr2 = telnet.sendCommand("act-user::Admin1:123::1Transport!;", ';');
        //System.out.println(reStr2.replaceFirst("act-user::Admin1:123::1Transport!;", ""));
        System.out.println(reStr2);
        String reStr3 = telnet.sendCommand("rtrv-inv::all:100:::;", ';');
        //System.out.println(reStr3.replaceFirst("rtrv-inv::all:100:::;", ""));
        System.out.println(reStr3);
        telnet.disconnect();

        def ignoreTypeList =['PORT_SHELF','HDPCT','PICO_SHELF','OPTICAL_SHELF','BFM']
        String invString = reStr3
        invString = invString.replace('\\','').replace('"','')
        def lineList = invString.findAll('.+?::TYPE=.+?,SN=.+?,PN=.+?,')
        lineList.each {
            String line = it
            def matcher = line =~ '(.+?)::TYPE=(.+?),SN=(.+?),PN=(.+?),'
            def location = matcher[0][1]
            def type = matcher[0][2]
            def sn = matcher[0][3]
            def pn = matcher[0][4]
            System.out.println(location + " " + type + " " + sn + " " + pn)
            System.out.println(type in ignoreTypeList)
        }

        System.out.println("End.")
    }
}
