package mutilthreaddemo

class CommandController {
    def scanService

    def index() { }

    def scan = {
        scanService.go()

        render "Done."
    }
}
