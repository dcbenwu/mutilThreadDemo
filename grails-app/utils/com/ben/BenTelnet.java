package com.ben;

import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.InputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;

/**
 * 使用apache的commons-net包模拟telnet登录
 */
public class BenTelnet {

    private TelnetClient telnet = null;
    private InputStream in;
    private PrintStream out;
    private char prompt = '#';  //linux提示符
    static final int ds_port = 3083;

    Logger log = LoggerFactory.getLogger(this.getClass().getName());


    public void free() {
        telnet = null;
        in = null;
        out = null;
    }

    /**
     * 登录linux
     * @param server
     * @param port
     * @param timeout milli seconds, default value 0 infinite, tested in windows7 the connecting estimated 20995ms
     */
    public BenTelnet(String server, int port, int timeout) throws Exception{

        // Connect to the specified server
        telnet = new TelnetClient();
        TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler(
                "VT100", false, false, true, false);
        EchoOptionHandler echoopt = new EchoOptionHandler(true, false,
                true, false);
        SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true,
                true, true, true);

        telnet.addOptionHandler(ttopt);
        telnet.addOptionHandler(echoopt);
        telnet.addOptionHandler(gaopt);

        telnet.setConnectTimeout(timeout);

        long begin = System.currentTimeMillis();
        try {
            telnet.connect(server, port);
        }catch (SocketTimeoutException ex) {
            long end = System.currentTimeMillis();
            //System.out.println("SocketTimeoutException estimate " + (end-begin) + " ms");
            //ex.printStackTrace();
            throw ex;
        } catch (ConnectException ex) {
            long end = System.currentTimeMillis();
            //System.out.println("ConnectException estimate " + (end-begin) + " ms");
            //ex.printStackTrace();
            System.err.println("ConnectException on ip " + server + " " + ex.getMessage() );
            throw ex;
        }

        // Get input and output stream references
        in = telnet.getInputStream();

        out = new PrintStream(telnet.getOutputStream());

        // Advance to a prompt
        System.out.println("try to Advance to a prompt on ip " + server);
        String retStr = readUntil('>' + " ");

    }

    public BenTelnet(String server, int port, int timeout, boolean debug) throws Exception{

        // Connect to the specified server
        telnet = new TelnetClient();
        TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler(
                "VT100", false, false, true, false);
        EchoOptionHandler echoopt = new EchoOptionHandler(true, false,
                true, false);
        SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true,
                true, true, true);

        telnet.addOptionHandler(ttopt);
        telnet.addOptionHandler(echoopt);
        telnet.addOptionHandler(gaopt);

        telnet.setConnectTimeout(timeout);

        long begin = System.currentTimeMillis();
        try {
            telnet.connect(server, port);
        }catch (SocketTimeoutException ex) {
            long end = System.currentTimeMillis();
            //System.out.println("SocketTimeoutException estimate " + (end-begin) + " ms");
            //ex.printStackTrace();
            throw ex;
        } catch (ConnectException ex) {
            long end = System.currentTimeMillis();
            //System.out.println("ConnectException estimate " + (end-begin) + " ms");
            //ex.printStackTrace();
            throw ex;
        }

        // Get input and output stream references
        in = telnet.getInputStream();

        out = new PrintStream(telnet.getOutputStream());

    }

    public BenTelnet(String server, String user, String password) {
        try {
            // Connect to the specified server
            telnet = new TelnetClient();
            TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler(
                    "VT100", false, false, true, false);
            EchoOptionHandler echoopt = new EchoOptionHandler(true, false,
                    true, false);
            SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true,
                    true, true, true);

            telnet.addOptionHandler(ttopt);
            telnet.addOptionHandler(echoopt);
            telnet.addOptionHandler(gaopt);

            telnet.connect(server, 23);

            // Get input and output stream references
            in = telnet.getInputStream();

            out = new PrintStream(telnet.getOutputStream());

            // Log the user on
            readUntil("login: ");
            write(user);

            readUntil("Password: ");
            write(password);

            // Advance to a prompt
            readUntil("$" + " ");

            // readUntil("$" + "su = root");
            // write("su - root");

        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 改变当前登录用户
     * @param user 用户名
     * @param password 密码
     * @param userTitle linux用户提示符
     * @return
     */
    public String suUser(String user, String password, String userTitle) throws Exception{
        // System.out.println("改变当前用户：");
        write("su - " + user);
        // System.out.println("准备读取返回的流，看是不是可以继续录入密码了：");
        readUntil("密码：");// 有可能不是中文，先用telnet命令测试下
        // System.out.println("返回信息提示可以录入密码，才开始录密码：");
        write(password);
        return readUntil(userTitle + " ");
    }

    /**
     * 读取流信息
     * @param pattern 流读取时的结束字符
     * @return
     */
    public String readUntil(String pattern) throws Exception{
        char lastChar = pattern.charAt(pattern.length() - 1);
        //System.out.println("当前流的字符集："+new InputStreamReader(in).getEncoding());
        StringBuffer sb = new StringBuffer();
        byte[] buff = new byte[1024];
        int ret_read = 0;
        String str = "";
        do {
            ret_read = in.read(buff);
            if (ret_read > 0) {
                // 把读取流的字符转码，可以在linux机子上用echo $LANG查看系统是什么编码
                String v = new String(buff, 0, ret_read, "UTF-8");
                str = str + v;
                //System.out.println("debug:"+str+"========"+pattern);
                if (str.contains(pattern)) {
                    //System.out.println("退出:"+str+"========"+pattern);
                    break;
                }
            }

        } while (ret_read >= 0);
        return str;
    }

    public String readUntil_4_rtrv(String pattern) throws Exception{
        char lastChar = pattern.charAt(pattern.length() - 1);
        //System.out.println("当前流的字符集："+new InputStreamReader(in).getEncoding());
        StringBuffer sb = new StringBuffer();
        byte[] buff = new byte[1024];
        int ret_read = 0;
        String str = "";
        do {
            ret_read = in.read(buff);
            if (ret_read > 0) {
                // 把读取流的字符转码，可以在linux机子上用echo $LANG查看系统是什么编码
                String v = new String(buff, 0, ret_read, "UTF-8");
                str = str + v;
                //System.out.println("debug:"+str+"========"+pattern);
                if (str.contains(pattern)) {
                    //System.out.println("退出:"+str+"========"+pattern);
                    if (str.contains("M  100 COMPLD"))
                        break;
                }
            }

        } while (ret_read >= 0);
        return str;
    }

    /**
     *
     * @param pattern
     * @param timeout  ms
     * @return
     * @throws Exception
     */
    public String readUntil_4_rtrv_with_timeout(String pattern, long timeout) throws Exception{
        long begin = System.currentTimeMillis();
        char lastChar = pattern.charAt(pattern.length() - 1);
        //System.out.println("当前流的字符集："+new InputStreamReader(in).getEncoding());
        StringBuffer sb = new StringBuffer();
        byte[] buff = new byte[1024];
        int ret_read = 0;
        String str = "";
        do {
            long delta = System.currentTimeMillis() - begin;
            if ( delta > timeout ) {
                str += "\n Error: Timeout";
                break;
            }
            //System.out.println("going to read buff...");
            ret_read = in.read(buff);
            if (ret_read > 0) {
                // 把读取流的字符转码，可以在linux机子上用echo $LANG查看系统是什么编码
                String v = new String(buff, 0, ret_read, "UTF-8");
                str = str + v;
                //System.out.println("v=====" +v);
                //System.out.println("debug:"+str+"========"+pattern);
                if (str.contains(pattern)) {
                    //System.out.println("退出:"+str+"========"+pattern);
                    break;
                }
            }

        } while (ret_read >= 0);
        return str;
    }

    /**
     * 向流中发送信息
     * @param value
     */
    public void write(String value) {
        try {
            out.println(value);
            out.flush();
            //System.out.println("录入命令:" + value);
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 运行命令，默认linux提示符是'$'
     * @param command 命令
     * @return
     */
    public String sendCommand(String command) {
        try {
            prompt = '$';
            write(command);
            return readUntil(prompt + " ");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 运行命令，默认linux提示符是'$'
     * @param command 命令
     * @param userTitle linux提示符
     * @return
     */
    public String sendCommand(String command, char userTitle) {
        try {
            prompt = userTitle;
            write(command);
            return readUntil(prompt + "");
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public String sendCommand(String command, String userTitle) {
        try {
            write(command);
            //Thread.sleep(200);
            return readUntil(userTitle + "");
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public String sendCommand_4_rtrv(String command, String userTitle, long timeout) {
        try {
            write(command);
            //Thread.sleep(200);
            return readUntil_4_rtrv_with_timeout(userTitle + "", timeout);
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 释放连接
     */
    public void disconnect() {
        try {
            telnet.disconnect();
        } catch (Exception e) {
            log.error(e.getMessage());
            System.err.println("IP: " + telnet.getRemoteAddress());
            e.printStackTrace();
        }
    }

    /**
     * scanOne
     * @param ip
     * @param timeOut
     */
    public void scanOne(String ip, int timeOut) {
        System.out.println("scanOne " + ip);
        String reStr = this.sendCommand("act-user::Admin1:123::*;", '>');
        System.out.println(reStr.replaceFirst("act-user::Admin1:123::*;", ""));

        String reStr2 = this.sendCommand("act-user::Admin1:123::1Transport!;", '>');
        System.out.println(reStr2.replaceFirst("act-user::Admin1:123::1Transport!;", ""));
        String reStr3 = this.sendCommand("rtrv-inv::all:100:::;", '>');
        System.out.println(reStr3.replaceFirst("rtrv-inv::all:100:::;", ""));
        this.disconnect();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            int count =0;
            //while (true) {
                System.err.println("--------" + (++count) + "--------" );
                // 172.29.160.15,
                BenTelnet telnet = new BenTelnet("172.29.151.72", 3083, 1000 * 3);
                //String reStr = telnet.sendCommand("act-user::Admin1:123::*;", ';');
                //System.out.println(reStr.replaceFirst("act-user::Admin1:123::*;", ""));
                //System.out.println(reStr);

                String reStr2 = telnet.sendCommand("act-user::Admin1:123::1Transport!;", ';');
                //System.out.println(reStr2.replaceFirst("act-user::Admin1:123::1Transport!;", ""));
                System.out.println(reStr2);
                String reStr3 = telnet.sendCommand_4_rtrv("rtrv-inv::all:100:::;", ";", 8000);
                //System.out.println(reStr3.replaceFirst("rtrv-inv::all:100:::;", ""));
                System.out.println(reStr3);
                telnet.disconnect();
                DeviceFilter deviceFilter = new DeviceFilter();
                deviceFilter.matchDeviceInfo(reStr3);
            //}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}