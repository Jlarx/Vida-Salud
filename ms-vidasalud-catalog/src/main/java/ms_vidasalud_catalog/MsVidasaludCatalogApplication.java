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

	@Bean
	CommandLineRunner initData(PrestacionRepository prestacionRepository) {
		return args -> {
			if (prestacionRepository.count() == 0) {
				Prestacion p1 = new Prestacion();
				p1.setNombre("Consulta Médica General");
				p1.setDescripcion("Atención primaria y evaluación médica inicial.");
				
				Prestacion p2 = new Prestacion();
				p2.setNombre("Cardiología Básica");
				p2.setDescripcion("Revisión de presión arterial y salud del corazón.");
				
				Prestacion p3 = new Prestacion();
				p3.setNombre("Toma de Muestras (Laboratorio)");
				p3.setDescripcion("Exámenes de sangre y orina rutinarios.");
				
				Prestacion p4 = new Prestacion();
				p4.setNombre("Kinesiología y Rehabilitación");
				p4.setDescripcion("Terapia física para recuperación de lesiones.");

				prestacionRepository.save(p1);
				prestacionRepository.save(p2);
				prestacionRepository.save(p3);
				prestacionRepository.save(p4);
				
				System.out.println("✅ Datos predeterminados de servicios médicos cargados exitosamente.");
			}
		};
	}

}
