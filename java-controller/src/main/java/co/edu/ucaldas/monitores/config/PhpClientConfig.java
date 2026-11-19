package co.edu.ucaldas.monitores.config;

import java.io.IOException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Crea el RestClient que usa PhpClient para hablar con el servicio PHP (PC2).
 * - La URL base sale de php.url (variable de entorno PHP_URL): nunca hay IPs en el código.
 * - Tiene tiempos de espera para que la interfaz no se quede colgada si PC2 no responde.
 * - Registra en el log cada petición reenviada (método, URL, estado, duración).
 */
@Configuration
public class PhpClientConfig {

    private static final Logger log = LoggerFactory.getLogger("reenvio-php");

    @Bean
    public RestClient phpRestClient(
            @Value("${php.url}") String phpUrl,
            @Value("${php.connect-timeout:3s}") Duration connectTimeout,
            @Value("${php.read-timeout:10s}") Duration readTimeout) {

        log.info("Servicio PHP configurado en {}", phpUrl);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(phpUrl)
                .requestFactory(factory)
                .requestInterceptor(registrarPeticion())
                .build();
    }

    /** Interceptor que escribe una línea de log por cada petición a PHP. */
    private ClientHttpRequestInterceptor registrarPeticion() {
        return (request, body, execution) -> {
            long inicio = System.currentTimeMillis();
            try {
                ClientHttpResponse response = execution.execute(request, body);
                log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getURI(),
                        response.getStatusCode().value(), System.currentTimeMillis() - inicio);
                return response;
            } catch (IOException e) {
                log.warn("{} {} -> SIN CONEXIÓN ({} ms): {}", request.getMethod(), request.getURI(),
                        System.currentTimeMillis() - inicio, e.getMessage());
                throw e;
            }
        };
    }
}
