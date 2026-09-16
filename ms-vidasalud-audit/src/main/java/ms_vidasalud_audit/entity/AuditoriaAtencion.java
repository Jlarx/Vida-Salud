package ms_vidasalud_audit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AuditoriaAtencion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long atencionId;
    
    @Enumerated(EnumType.STRING)
    private EstadoAtencion estadoAnterior;
    
    @Enumerated(EnumType.STRING)
    private EstadoAtencion estadoNuevo;
    
    private LocalDateTime fechaCambio;
    private String usuarioEmail;

    @PrePersist
    protected void onCreate() {
        fechaCambio = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAtencionId() { return atencionId; }
    public void setAtencionId(Long atencionId) { this.atencionId = atencionId; }
    public EstadoAtencion getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoAtencion estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoAtencion getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoAtencion estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public LocalDateTime getFechaCambio() { return fechaCambio; }
    public void setFechaCambio(LocalDateTime fechaCambio) { this.fechaCambio = fechaCambio; }
    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }
}
