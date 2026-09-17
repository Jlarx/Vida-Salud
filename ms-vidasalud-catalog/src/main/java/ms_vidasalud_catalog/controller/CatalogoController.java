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
@PreAuthorize("hasRole('ADMINISTRADOR')") // Solo Administradores pueden gestionar el catálogo
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
    public Box createBox(@RequestBody Box box) {
        return boxRepository.save(box);
    }

    @GetMapping("/prestaciones")
    public List<Prestacion> getPrestaciones() {
        return prestacionRepository.findAll();
    }

    @PostMapping("/prestaciones")
    public Prestacion createPrestacion(@RequestBody Prestacion prestacion) {
        return prestacionRepository.save(prestacion);
    }
}
