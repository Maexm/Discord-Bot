package util;

import java.time.Duration;

import reactor.core.publisher.Mono;

public class AsyncTasks {
    
    public final static Mono<Void> wait(Duration duration){
        return Mono.delay(duration).then();
    }
}
