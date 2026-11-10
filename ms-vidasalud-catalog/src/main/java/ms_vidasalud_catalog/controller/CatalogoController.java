package ms_vidasalud_catalog.controller;

import ms_vidasalud_catalog.entity.Box;
import ms_vidasalud_catalog.entity.Prestacion;
import ms_vidasalud_catalog.repository.BoxRepository;
import ms_vidasalud_catalog.repository.PrestacionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/catalog", "/catalog"})
public class CatalogoController {

    private final BoxRepository boxRepository;
    private final PrestacionRepository prestacionRepository;

    public CatalogoController(BoxRepository boxRepository, PrestacionRepository prestacionRepository) {
        this.boxRepository = boxRepository;
        this.prestacionRepository = prestacionRepository;
    }

    // Boxes
    @GetMapping("/boxes")
    public List<Box> getBoxes() {
        return boxRepository.findAll();
    }

    @GetMapping("/boxes/{id}")
    public Box getBoxById(@PathVariable Long id) {
        return boxRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Box no encontrado con ID " + id));
    }

    @PostMapping("/boxes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Box createBox(@RequestBody Box box) {
        return boxRepository.save(box);
    }

    @PutMapping("/boxes/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Box updateBox(@PathVariable Long id, @RequestBody Box boxActualizado) {
        return boxRepository.findById(id).map(box -> {
            if (boxActualizado.getNombre() != null) box.setNombre(boxActualizado.getNombre());
            if (boxActualizado.getNumero() != null) box.setNumero(boxActualizado.getNumero());
            if (boxActualizado.getCuposTotales() != null) box.setCuposTotales(boxActualizado.getCuposTotales());
            if (boxActualizado.getCuposDisponibles() != null) box.setCuposDisponibles(boxActualizado.getCuposDisponibles());
            return boxRepository.save(box);
        }).orElseThrow(() -> new RuntimeException("Box no encontrado con ID " + id));
    }

    @DeleteMapping("/boxes/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void deleteBox(@PathVariable Long id) {
        boxRepository.deleteById(id);
    }

    // Prestaciones
    @GetMapping("/prestaciones")
    public List<Prestacion> getPrestaciones() {
        return prestacionRepository.findAll();
    }

    @GetMapping("/prestaciones/{id}")
    public Prestacion getPrestacionById(@PathVariable Long id) {
        return prestacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prestación no encontrada con ID " + id));
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
