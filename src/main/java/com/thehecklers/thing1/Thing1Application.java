package com.thehecklers.thing1;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.time.Instant;

@SpringBootApplication
@TypeHint(types = {Weather.class, Aircraft.class},
        typeNames = {"com.thehecklers.thing1.Weather", "com.thehecklers.thing1.Aircraft"})
public class Thing1Application {

    public static void main(String[] args) {
        Hooks.onErrorDropped(e -> System.out.println(" Client disconnected, bye for now! "));
        SpringApplication.run(Thing1Application.class, args);
    }

    @Bean
    RSocketRequester requester(RSocketRequester.Builder builder) {
        return builder.tcp("192.168.86.168", 7635);
    }
}

@Controller
@AllArgsConstructor
class Thing1Controller {
    private final RSocketRequester requester;

    // Request/response
    @MessageMapping("reqresp")
    Mono<Aircraft> reqResp(Mono<Instant> tsMono) {
        return tsMono.doOnNext(ts -> System.out.println("⏰ " + ts))
                .then(requester.route("acstream")
                        .data(Instant.now())
                        .retrieveFlux(Aircraft.class)
                        .next());
    }

    // Request/stream
    @MessageMapping("reqstream")
    Flux<Aircraft> reqStream(Mono<Instant> tsMono) {
        return tsMono.doOnNext(ts -> System.out.println("⏰ " + ts))
                .thenMany(requester.route("acstream")
                        .data(Instant.now())
                        .retrieveFlux(Aircraft.class));
    }

    // Fire and forget
    @MessageMapping("fireforget")
    Mono<Void> fireAndForget(Mono<Weather> weatherMono) {
        return weatherMono.doOnNext(wx -> System.out.println("☁️ " + wx))
                .then();
    }

    // Bidirectional channel
    @MessageMapping("channel")
    Flux<Aircraft> channel(Flux<Weather> weatherFlux) {
        return weatherFlux.doOnSubscribe(sub -> System.out.println("🚨 Subscribed to weather! 📰"))
                .doOnNext(wx -> System.out.println("☀️ " + wx))
                .switchMap(wx -> requester.route("acstream")
                        .data(Instant.now())
                        .retrieveFlux(Aircraft.class));
    }

}

@Data
class Weather {
    private String flight_rules, raw;
}

@Data
class Aircraft {
    private String callsign, reg, flightno, type;
    private int altitude, heading, speed;
    private double lat, lon;
}