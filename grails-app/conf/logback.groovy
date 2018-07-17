import grails.util.BuildSettings
import grails.util.Environment
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import java.nio.charset.Charset

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName('UTF-8')

        pattern =
                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
                        '%clr(%5p) ' + // Log level
                        '%clr(---){faint} %clr([%15.15t]){faint} ' + // Thread
                        '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                        '%m%n%wex' // Message
    }
}

def targetDir = System.getenv().get("cats_log_dir")
if ( ! targetDir ) {
    targetDir = BuildSettings.TARGET_DIR
}

appender("FILE", RollingFileAppender) {
    file = "${targetDir}/beninfo.log"
    append = true
    rollingPolicy(FixedWindowRollingPolicy) {
        fileNamePattern = "${targetDir}/beninfo.%i.log.zip"
        minIndex = 1
        maxIndex = 300
    }
    triggeringPolicy(SizeBasedTriggeringPolicy) {
        maxFileSize = "100MB"
    }
    encoder(PatternLayoutEncoder) {
        //pattern = "%-4relative [%thread] %-5level %logger{35} - %msg%n"
        pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    }
}
//root(ERROR, ['STDOUT'])
root(INFO, ['STDOUT', 'FILE'])
