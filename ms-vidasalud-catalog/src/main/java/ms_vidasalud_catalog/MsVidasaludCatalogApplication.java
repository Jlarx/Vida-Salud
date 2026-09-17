package ms_vidasalud_catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ms_vidasalud_catalog.entity.Prestacion;
import ms_vidasalud_catalog.repository.PrestacionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MsVidasaludCatalogApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsVidasaludCatalogApplication.class, args);
	}

}
