import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

public class WebClientAPI {
    private final WebClient webClient;

    public WebClientAPI() {
        //this.webClient = WebClient.create("http://localhost:8080/products");
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120)).build();
        this.webClient =WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider)))
                .baseUrl("http://localhost:8080/products")
                .build();
    }

    public static void main(String args[]) {
        WebClientAPI api = new WebClientAPI();

        api.postNewProduct()
                .thenMany(api.getAllProducts())
                .take(1)
                .flatMap(p -> api.updateProduct(p.getId(), "White Tea", 0.99))
                .flatMap(p -> api.deleteProduct(p.getId()))
                .thenMany(api.getAllProducts())
                .thenMany(api.getAllEvents())
                .subscribeOn(Schedulers.newSingle("myThread"))
                .subscribe(System.out::println);

//        try {
//            Thread.sleep(5000);
//        } catch(Exception e) { }
    }

    private Mono<ResponseEntity<Product>> postNewProduct() {
        return webClient
                .post()
                .body(Mono.just(new Product(null, "Jasmine Tea", 1.99)), Product.class)
                .exchangeToMono(response -> response.toEntity(Product.class))
                .doOnSuccess(o -> System.out.println("**********POST " + o));
    }

    private Flux<Product> getAllProducts() {
        return webClient
                .get()
                .retrieve()
                .bodyToFlux(Product.class)
                .doOnNext(o -> System.out.println("**********GET: " + o));
    }

    private Mono<Product> updateProduct(String id, String name, double price) {
        return webClient
                .put()
                .uri("/{id}", id)
                .body(Mono.just(new Product(null, name, price)), Product.class)
                .retrieve()
                .bodyToMono(Product.class)
                .doOnSuccess(o -> System.out.println("**********UPDATE " + o));
    }

    private Mono<Void> deleteProduct(String id) {
        return webClient
                .delete()
                .uri("/{id}", id)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(o -> System.out.println("**********DELETE " + o));
    }

    private Flux<ProductEvent> getAllEvents() {
        return webClient
                .get()
                .uri("/events")
                .retrieve()
                .bodyToFlux(ProductEvent.class);
    }
}
