package ms_vidasalud_catalog.repository;

import ms_vidasalud_catalog.entity.Prestacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrestacionRepository extends JpaRepository<Prestacion, Long> {
}
