import { useState, useEffect } from 'react';
import apiClient from '../apiClient';

export default function PatientDashboard() {
  const [atenciones, setAtenciones] = useState([]);
  const [prestaciones, setPrestaciones] = useState([]);
  const [newAtencion, setNewAtencion] = useState({ prestacionId: '', boxId: 1 });
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const [resAtenciones, resPrestaciones] = await Promise.allSettled([
        apiClient.get('/appointments'),
        apiClient.get('/catalog/prestaciones')
      ]);

      if (resAtenciones.status === 'fulfilled') {
        setAtenciones(Array.isArray(resAtenciones.value.data) ? resAtenciones.value.data : []);
      } else {
        console.error("Error fetching appointments:", resAtenciones.reason);
      }

      if (resPrestaciones.status === 'fulfilled') {
        const prestList = Array.isArray(resPrestaciones.value.data) ? resPrestaciones.value.data : [];
        setPrestaciones(prestList);
        if (prestList.length > 0 && !newAtencion.prestacionId) {
          setNewAtencion(prev => ({ ...prev, prestacionId: String(prestList[0].id) }));
        }
      } else {
        console.error("Error fetching catalog:", resPrestaciones.reason);
        setErrorMsg("No se pudieron cargar los servicios médicos del catálogo. Revisa la conexión con el servidor.");
      }
    } catch (err) {
      console.error("Error general al cargar datos:", err);
      setErrorMsg("Error de comunicación al contactar la plataforma.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreateAtencion = async (e) => {
    e.preventDefault();
    if (!newAtencion.prestacionId) {
      alert("Por favor seleccione un servicio médico.");
      return;
    }
    setIsSubmitting(true);
    try {
      await apiClient.post('/appointments', {
        prestacionId: Number(newAtencion.prestacionId),
        boxId: Number(newAtencion.boxId) || 1
      });
      await fetchData();
      alert("¡Cita agendada con éxito!");
    } catch (error) {
      console.error("Error creating appointment", error);
      alert("Error al agendar cita: " + (error.response?.data?.message || error.message));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <h2 className="section-title">Mis Citas Médicas</h2>

      {errorMsg && (
        <div style={{
          padding: '1rem',
          marginBottom: '1.5rem',
          backgroundColor: '#ffebee',
          color: '#c62828',
          borderRadius: '8px',
          border: '1px solid #ef9a9a',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <span>⚠️ {errorMsg}</span>
          <button 
            type="button" 
            className="btn-action" 
            onClick={fetchData}
            style={{ padding: '0.3rem 0.8rem', fontSize: '0.85rem' }}
          >
            Reintentar
          </button>
        </div>
      )}
      
      <div className="card-white" style={{ marginBottom: '2rem' }}>
        <h3 style={{ marginTop: 0, color: '#00a4ef' }}>Agendar Nueva Cita</h3>
        <form onSubmit={handleCreateAtencion} className="clean-form">
          <select 
            className="clean-input"
            value={newAtencion.prestacionId} 
            onChange={e => setNewAtencion({...newAtencion, prestacionId: e.target.value})} 
            required
            disabled={loading || prestaciones.length === 0}
          >
            {loading ? (
              <option value="">Cargando servicios médicos disponibles...</option>
            ) : prestaciones.length === 0 ? (
              <option value="">No hay servicios médicos registrados en el catálogo</option>
            ) : (
              <>
                <option value="">Seleccione el servicio médico que necesita...</option>
                {prestaciones.map(p => (
                  <option key={p.id} value={p.id}>{p.nombre} - {p.descripcion}</option>
                ))}
              </>
            )}
          </select>
          <button 
            type="submit" 
            className="btn-action" 
            disabled={isSubmitting || loading || prestaciones.length === 0}
          >
            {isSubmitting ? "Agendando..." : "Agendar Cita"}
          </button>
        </form>
      </div>

      <div className="card-white">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h3 style={{ margin: 0, color: '#00a4ef' }}>Historial de Citas</h3>
          <button 
            type="button" 
            className="btn-action" 
            onClick={fetchData} 
            disabled={loading}
            style={{ padding: '0.4rem 0.8rem', fontSize: '0.9rem' }}
          >
            {loading ? "Cargando..." : "🔄 Actualizar"}
          </button>
        </div>
        <table className="modern-table">
          <thead>
            <tr>
              <th>ID Cita</th>
              <th>Servicio (ID Prestación)</th>
              <th>Fecha Registro</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            {atenciones.length > 0 ? atenciones.map(a => (
              <tr key={a.id}>
                <td><strong>#{a.id}</strong></td>
                <td>Servicio #{a.prestacionId}</td>
                <td>{a.fechaCreacion ? new Date(a.fechaCreacion).toLocaleString() : 'Reciente'}</td>
                <td><span className={`status-pill status-${a.estado}`}>{a.estado}</span></td>
              </tr>
            )) : (
              <tr><td colSpan="4" style={{ textAlign: 'center', color: '#7f8c8d' }}>
                {loading ? "Cargando citas..." : "Aún no tienes citas agendadas."}
              </td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
