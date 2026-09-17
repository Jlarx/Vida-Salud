package ms_vidasalud_catalog.controller;

import ms_vidasalud_catalog.entity.Box;
import ms_vidasalud_catalog.entity.Prestacion;
import ms_vidasalud_catalog.repository.BoxRepository;
import ms_vidasalud_catalog.repository.PrestacionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogoController {

    private final BoxRepository boxRepository;
    private final PrestacionRepository prestacionRepository;

    public CatalogoController(BoxRepository boxRepository, PrestacionRepository prestacionRepository) {
        this.boxRepository = boxRepository;
        this.prestacionRepository = prestacionRepository;
    }

    @GetMapping("/boxes")
    public List<Box> getBoxes() {
        return boxRepository.findAll();
    }

    @PostMapping("/boxes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Box createBox(@RequestBody Box box) {
        return boxRepository.save(box);
    }

    @GetMapping("/prestaciones")
    public List<Prestacion> getPrestaciones() {
        return prestacionRepository.findAll();
    }

    @PostMapping("/prestaciones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Prestacion createPrestacion(@RequestBody Prestacion prestacion) {
        return prestacionRepository.save(prestacion);
    }

    @PutMapping("/prestaciones/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Prestacion updatePrestacion(@PathVariable Long id, @RequestBody Prestacion prestacionActualizada) {
        return prestacionRepository.findById(id).map(prestacion -> {
            prestacion.setNombre(prestacionActualizada.getNombre());
            prestacion.setDescripcion(prestacionActualizada.getDescripcion());
            return prestacionRepository.save(prestacion);
        }).orElseThrow(() -> new RuntimeException("Prestación no encontrada"));
    }

    @DeleteMapping("/prestaciones/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void deletePrestacion(@PathVariable Long id) {
        prestacionRepository.deleteById(id);
    }
}
