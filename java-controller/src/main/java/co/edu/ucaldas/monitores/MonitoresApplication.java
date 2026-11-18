package co.edu.ucaldas.monitores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del controlador Java (PC1).
 * Arranca Tomcat en el puerto 8080 y sirve tanto la API (/api/**) como la interfaz (static/).
 */
@SpringBootApplication
public class MonitoresApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoresApplication.class, args);
    }
}
