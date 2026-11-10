package ms_vidasalud_appointments;

import ms_vidasalud_appointments.entity.Atencion;
import ms_vidasalud_appointments.entity.EstadoAtencion;
import ms_vidasalud_appointments.repository.AtencionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class MsVidasaludAppointmentsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsVidasaludAppointmentsApplication.class, args);
	}

	@Bean
	CommandLineRunner seedAppointments(AtencionRepository atencionRepository) {
		return args -> {
			if (atencionRepository.count() == 0) {
				Atencion a1 = new Atencion();
				a1.setPacienteEmail("paciente@vidasalud.cl");
				a1.setEstado(EstadoAtencion.CREADA);
				a1.setPrestacionId(1L);
				a1.setBoxId(1L);
				a1.setFechaCita(LocalDateTime.now().plusDays(1).withHour(9).withMinute(30).withSecond(0));
				atencionRepository.save(a1);

				Atencion a2 = new Atencion();
				a2.setPacienteEmail("paciente@vidasalud.cl");
				a2.setEstado(EstadoAtencion.CONFIRMADA);
				a2.setPrestacionId(2L);
				a2.setBoxId(2L);
				a2.setFechaCita(LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0));
				atencionRepository.save(a2);

				Atencion a3 = new Atencion();
				a3.setPacienteEmail("juan.perez@email.com");
				a3.setEstado(EstadoAtencion.ATENDIDA);
				a3.setPrestacionId(1L);
				a3.setBoxId(1L);
				a3.setFechaCita(LocalDateTime.now().minusDays(1).withHour(15).withMinute(0).withSecond(0));
				atencionRepository.save(a3);
			}
		};
	}

}
