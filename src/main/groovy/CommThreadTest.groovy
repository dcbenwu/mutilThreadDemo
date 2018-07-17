class CommThreadTest {

    public static void main(args) {
        def mytask = { println "3.hello" }

        println "1.begin"

        new Thread({
            mytask()
        }).start()

        println "2.end"
    }
}
