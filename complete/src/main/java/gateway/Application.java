package gateway;

import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpServerErrorException;
import reactor.core.publisher.Mono;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

// tag::code[]
@SpringBootApplication
@EnableConfigurationProperties(UriConfiguration.class)
@RestController
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // tag::route-locator[]
    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder, UriConfiguration uriConfiguration) {
        String httpUri = uriConfiguration.getHttpbin();
        return builder.routes()
            .route(p -> p
                .path("/get")
                .filters(f -> f.addRequestHeader("Hello", "World"))
                .uri(httpUri))
            .route(p -> p
                .host("*.hystrix.com")
                .filters(f -> f
                    .hystrix(config -> config
                        .setName("mycmd")
                        .setFallbackUri("forward:/fallback")))
                .uri(httpUri))
            .build();
    }
    // end::route-locator[]

    // tag::fallback[]
    @RequestMapping("/fallback")
    public Mono<String> fallback() {
        return Mono.just("fallback");
    }
    // end::fallback[]

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login() {
        return "<body><div style=\"background: #4cccdd; width: 60%; padding: 50px; margin-bottom: 10px;\">Login</div>" +
                "<form action=\"welcome\" method=\"GET\">Username: <input type=\"text\" name=\"username\" />" +
                "<input type=\"submit\" />" +
                "</form>" +
                "</body>";
    }

    @RequestMapping(value = "/welcome", method = RequestMethod.GET)
    public String welcome(@RequestParam String username) {
        return "<body><div style=\"background: #cccccc; width: 60%; padding: 50px;\">Welcome " +
                "<div style=\"color: #cc4499;\"> " + username + "</div>" +
                "</div>" +
                "</body>";
    }

    @RequestMapping(produces = APPLICATION_JSON_VALUE, path="/svc/P107_PROXY_ToPo/restadapter/sender/azuraapi/i800/704/v2/probeobjectwithinrange/probeobjectwithinrangerequest")
    public Mono<String> getPoGrid(
            @RequestParam(name = "Easting", required = true)
                    Double easting,

            @RequestParam(name = "Northing", required = true)
                    Double northing,

            @RequestParam(name = "SpatialReferenceSystemIdentifier", required = true)
                    Integer spatialReferenceSystemIdentifier
    ) {
        if (spatialReferenceSystemIdentifier == 0) {
            return Mono.error(NotFoundException.create(true, "Resource not found"));
        } else if (spatialReferenceSystemIdentifier == 1) {
            return Mono.error(
                    HttpServerErrorException.create(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "PO internal server error",
                            new HttpHeaders(new LinkedMultiValueMap(Map.of("POMESSAGEID", "44625"))),
                            new byte[10],
                            null
                    )
            );
        }

        if (easting == 0 && northing == 0) {
            return Mono.just("{\"errorMessage\": \"Spatial geolocation service unavailable. Please try again.\"}");
        } else if (easting <= 10 && northing <= 10) {
            return Mono.just("{\"success\": \"true\", " +
                    "\"objectFound\": \"true\"" +
                    "}");
        }

        return Mono.just("{\"success\": \"true\", " +
                "\"objectFound\": \"false\"" +
                "}");
    }

}

// tag::uri-configuration[]
@ConfigurationProperties
class UriConfiguration {
    
    private String httpbin = "http://httpbin.org:80";

    public String getHttpbin() {
        return httpbin;
    }

    public void setHttpbin(String httpbin) {
        this.httpbin = httpbin;
    }
}
// end::uri-configuration[]
// end::code[]