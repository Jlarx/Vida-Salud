package ms_vidasalud_catalog.entity;

import jakarta.persistence.*;

@Entity
public class Box {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    private String numero;
    private Integer cuposTotales;
    private Integer cuposDisponibles;

    @PrePersist
    @PreUpdate
    protected void onPersist() {
        if (numero != null && (nombre == null || nombre.isBlank())) {
            nombre = "Box " + numero;
        } else if (nombre != null && (numero == null || numero.isBlank())) {
            numero = nombre.replaceAll("[^0-9]", "");
            if (numero.isBlank()) numero = nombre;
        }
        if (cuposTotales == null && cuposDisponibles != null) {
            cuposTotales = cuposDisponibles;
        } else if (cuposDisponibles == null && cuposTotales != null) {
            cuposDisponibles = cuposTotales;
        }
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { 
        return nombre != null ? nombre : (numero != null ? "Box " + numero : ""); 
    }
    public void setNombre(String nombre) { 
        this.nombre = nombre; 
        if (this.numero == null || this.numero.isBlank()) {
            this.numero = nombre;
        }
    }
    
    public String getNumero() { 
        return numero != null ? numero : nombre; 
    }
    public void setNumero(String numero) { 
        this.numero = numero; 
        if (this.nombre == null || this.nombre.isBlank()) {
            this.nombre = "Box " + numero;
        }
    }
    
    public Integer getCuposTotales() { return cuposTotales; }
    public void setCuposTotales(Integer cuposTotales) { this.cuposTotales = cuposTotales; }
    
    public Integer getCuposDisponibles() { return cuposDisponibles; }
    public void setCuposDisponibles(Integer cuposDisponibles) { this.cuposDisponibles = cuposDisponibles; }
}
