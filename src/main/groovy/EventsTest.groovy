import grails.events.Events
import reactor.spring.context.annotation.Consumer
import reactor.spring.context.annotation.Selector



@Consumer
class Game implements Events{
    @Selector("play")
    def doPlay(Object msg) {
        println(msg.toString())
    }
}

def eventsTestScenarioService

notify("play", "first play")