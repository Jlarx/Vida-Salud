import { useState, useEffect } from 'react';
import apiClient from '../apiClient';

export default function ReceptionDashboard() {
  const [atenciones, setAtenciones] = useState([]);
  const [editFecha, setEditFecha] = useState({});
  const estados = ['SOLICITADA', 'CONFIRMADA', 'EN_ESPERA', 'EN_ATENCION', 'CERRADA', 'CANCELADA'];

  const fetchData = async () => {
    try {
      const res = await apiClient.get('/appointments');
      setAtenciones(res.data);
    } catch (error) {
      console.error("Error fetching appointments", error);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleChangeStatus = async (id, nuevoEstado) => {
    try {
      await apiClient.put(`/appointments/${id}/estado?nuevoEstado=${nuevoEstado}`);
      fetchData();
      alert(`Estado de la cita #${id} actualizado a ${nuevoEstado}`);
    } catch (error) {
      console.error("Error changing status", error);
      alert("Error al cambiar estado: " + (error.response?.data?.message || error.message));
    }
  };

  const handleReagendar = async (id) => {
    const fechaHora = editFecha[id];
    if (!fechaHora) {
      alert("Seleccione una nueva fecha y hora para reagendar.");
      return;
    }
    try {
      await apiClient.put(`/appointments/${id}/reagendar?nuevaFecha=${fechaHora}`);
      fetchData();
      alert(`¡Cita #${id} reagendada con éxito para ${fechaHora.replace('T', ' ')}!`);
    } catch (error) {
      console.error("Error al reagendar", error);
      alert("Error al reagendar cita: " + (error.response?.data?.message || error.message));
    }
  };

  return (
    <div>
      <h2 className="section-title">Gestión de Citas (Recepción y Operador)</h2>
      
      <div className="card-white">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h3 style={{ margin: 0, color: '#00a4ef' }}>Citas Médicas Registradas</h3>
          <button className="btn-action" onClick={fetchData} style={{ padding: '0.4rem 0.8rem', fontSize: '0.9rem' }}>
            🔄 Actualizar Citas
          </button>
        </div>
        <table className="modern-table">
          <thead>
            <tr>
              <th>ID Cita</th>
              <th>Paciente (Email)</th>
              <th>Servicio</th>
              <th>Fecha Programada</th>
              <th>Estado Actual</th>
              <th>Acción Estado</th>
              <th>Reagendar Fecha / Hora (Imprevistos)</th>
            </tr>
          </thead>
          <tbody>
            {atenciones.length > 0 ? atenciones.map(a => (
              <tr key={a.id}>
                <td><strong>#{a.id}</strong></td>
                <td>{a.pacienteEmail}</td>
                <td>Prestación #{a.prestacionId}</td>
                <td>
                  <strong>{a.fechaCita ? a.fechaCita.replace('T', ' ').substring(0, 16) : 'Pendiente'}</strong>
                </td>
                <td><span className={`status-pill status-${a.estado}`}>{a.estado}</span></td>
                <td>
                  <select 
                    className="select-status" 
                    onChange={(e) => handleChangeStatus(a.id, e.target.value)}
                    value={a.estado}
                  >
                    {estados.map(est => <option key={est} value={est}>{est}</option>)}
                  </select>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <input 
                      type="datetime-local" 
                      className="clean-input"
                      style={{ padding: '0.3rem', fontSize: '0.85rem', width: '190px' }}
                      value={editFecha[a.id] || ''}
                      onChange={(e) => setEditFecha({ ...editFecha, [a.id]: e.target.value })}
                    />
                    <button 
                      className="btn-action" 
                      style={{ padding: '0.4rem 0.7rem', fontSize: '0.85rem', whiteSpace: 'nowrap' }}
                      onClick={() => handleReagendar(a.id)}
                    >
                      Guardar
                    </button>
                  </div>
                </td>
              </tr>
            )) : (
              <tr><td colSpan="7" style={{ textAlign: 'center', color: '#7f8c8d' }}>No hay citas registradas en el sistema.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
