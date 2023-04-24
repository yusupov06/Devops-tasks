package uz.md.apilimiter.resource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.md.apilimiter.aop.ApiLimited;


@RestController
@ConditionalOnProperty(
        prefix = "api-resource",
        name = {"simulate"},
        havingValue = "true",
        matchIfMissing = true
)
@RequestMapping(SimulationResource.SIMULATION_ENDPOINT)
public class SimulationResource {

    public static final String SIMULATION_ENDPOINT = "/simulation";

    @ApiLimited
    @GetMapping("/api-1")
    public String api1() {
        return "api-1";
    }


    @ApiLimited
    @GetMapping("/api-2")
    public String api2() {
        return "api-2";
    }


    @ApiLimited
    @GetMapping("/api-3")
    public String api3() {
        return "api-3";
    }



    @ApiLimited
    @GetMapping("/services/1")
    public String servicesApi1() {
        return "services-1";
    }


    @ApiLimited
    @GetMapping("/services/2")
    public String servicesApi2() {
        return "services-2";
    }


    @ApiLimited
    @GetMapping("/services/3")
    public String servicesApi3() {
        return "services-3";
    }



}
