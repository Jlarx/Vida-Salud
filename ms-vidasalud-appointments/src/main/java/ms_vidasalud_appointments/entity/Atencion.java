package ms_vidasalud_appointments.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Atencion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String pacienteEmail;
    
    @Enumerated(EnumType.STRING)
    private EstadoAtencion estado;
    
    private Long prestacionId;
    
    private Long boxId;
    
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPacienteEmail() { return pacienteEmail; }
    public void setPacienteEmail(String pacienteEmail) { this.pacienteEmail = pacienteEmail; }
    public EstadoAtencion getEstado() { return estado; }
    public void setEstado(EstadoAtencion estado) { this.estado = estado; }
    public Long getPrestacionId() { return prestacionId; }
    public void setPrestacionId(Long prestacionId) { this.prestacionId = prestacionId; }
    public Long getBoxId() { return boxId; }
    public void setBoxId(Long boxId) { this.boxId = boxId; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
