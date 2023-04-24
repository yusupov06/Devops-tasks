package uz.md.synccachereactive;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
public class SyncCacheReactiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncCacheReactiveApplication.class, args);
    }

}
