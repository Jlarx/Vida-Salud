import { useState, useEffect } from 'react';
import apiClient from '../apiClient';

export default function PatientDashboard() {
  const [atenciones, setAtenciones] = useState([]);
  const [prestaciones, setPrestaciones] = useState([]);
  const [newAtencion, setNewAtencion] = useState({ prestacionId: '', boxId: 1 }); 

  const fetchData = async () => {
    try {
      const [resAtenciones, resPrestaciones] = await Promise.all([
        apiClient.get('/appointments'),
        apiClient.get('/catalog/prestaciones')
      ]);
      setAtenciones(resAtenciones.data);
      setPrestaciones(resPrestaciones.data);
    } catch (error) {
      console.error("Error fetching patient data", error);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreateAtencion = async (e) => {
    e.preventDefault();
    try {
      await apiClient.post('/appointments', newAtencion);
      fetchData();
    } catch (error) {
      console.error("Error creating appointment", error);
    }
  };

  return (
    <div>
      <h2 className="section-title">Mis Citas Médicas</h2>
      
      <div className="card-white" style={{ marginBottom: '2rem' }}>
        <h3 style={{ marginTop: 0, color: '#00a4ef' }}>Agendar Nueva Cita</h3>
        <form onSubmit={handleCreateAtencion} className="clean-form">
          <select 
            className="clean-input"
            value={newAtencion.prestacionId} 
            onChange={e => setNewAtencion({...newAtencion, prestacionId: e.target.value})} 
            required
          >
            <option value="">Seleccione el servicio médico que necesita...</option>
            {prestaciones.map(p => (
              <option key={p.id} value={p.id}>{p.nombre} - {p.descripcion}</option>
            ))}
          </select>
          <button type="submit" className="btn-action">Agendar Cita</button>
        </form>
      </div>

      <div className="card-white">
        <h3 style={{ marginTop: 0, color: '#00a4ef' }}>Historial de Citas</h3>
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
              <tr><td colSpan="4" style={{ textAlign: 'center', color: '#7f8c8d' }}>Aún no tienes citas agendadas.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
