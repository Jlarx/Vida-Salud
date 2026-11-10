package ms_vidasalud_catalog.config;

import ms_vidasalud_catalog.entity.Box;
import ms_vidasalud_catalog.entity.Prestacion;
import ms_vidasalud_catalog.repository.BoxRepository;
import ms_vidasalud_catalog.repository.PrestacionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initCatalogData(BoxRepository boxRepo, PrestacionRepository prestacionRepo) {
        return args -> {
            if (boxRepo.count() == 0) {
                Box box1 = new Box();
                box1.setNumero("1");
                box1.setNombre("Box General 1");
                box1.setCuposTotales(10);
                box1.setCuposDisponibles(10);
                boxRepo.save(box1);

                Box box2 = new Box();
                box2.setNumero("2");
                box2.setNombre("Box Pediatría 2");
                box2.setCuposTotales(10);
                box2.setCuposDisponibles(10);
                boxRepo.save(box2);

                Box box3 = new Box();
                box3.setNumero("3");
                box3.setNombre("Box Urgencias 3");
                box3.setCuposTotales(10);
                box3.setCuposDisponibles(10);
                boxRepo.save(box3);
            }

            if (prestacionRepo.count() == 0) {
                Prestacion p1 = new Prestacion();
                p1.setNombre("Consulta Medicina General");
                p1.setDescripcion("Atención médica preventiva y de diagnóstico general.");
                prestacionRepo.save(p1);

                Prestacion p2 = new Prestacion();
                p2.setNombre("Atención Pediátrica");
                p2.setDescripcion("Control de niño sano y diagnóstico pediátrico.");
                prestacionRepo.save(p2);

                Prestacion p3 = new Prestacion();
                p3.setNombre("Consulta Cardiológica");
                p3.setDescripcion("Evaluación del sistema cardiovascular y electrocardiograma.");
                prestacionRepo.save(p3);

                Prestacion p4 = new Prestacion();
                p4.setNombre("Control Nutricional");
                p4.setDescripcion("Evaluación antropométrica y pauta alimentaria personalizada.");
                prestacionRepo.save(p4);
            }
        };
    }
}

