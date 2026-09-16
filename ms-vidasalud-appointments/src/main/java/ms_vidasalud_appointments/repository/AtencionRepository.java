package ms_vidasalud_appointments.repository;

import ms_vidasalud_appointments.entity.Atencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtencionRepository extends JpaRepository<Atencion, Long> {
}
