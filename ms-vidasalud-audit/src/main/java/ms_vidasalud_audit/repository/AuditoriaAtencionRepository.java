package ms_vidasalud_audit.repository;

import ms_vidasalud_audit.entity.AuditoriaAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaAtencionRepository extends JpaRepository<AuditoriaAtencion, Long> {
    List<AuditoriaAtencion> findByAtencionIdOrderByFechaCambioDesc(Long atencionId);
}
