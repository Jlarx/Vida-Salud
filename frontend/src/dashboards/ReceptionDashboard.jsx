import { useState, useEffect } from 'react';
import apiClient from '../apiClient';

export default function ReceptionDashboard() {
  const [atenciones, setAtenciones] = useState([]);
  const estados = ['SOLICITADA', 'CONFIRMADA', 'EN_ESPERA', 'ATENDIDA'];

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
    } catch (error) {
      console.error("Error changing status", error);
    }
  };

  return (
    <div>
      <h2 className="section-title">Gestión de Citas (Recepción)</h2>
      
      <div className="card-white">
        <h3 style={{ marginTop: 0, color: '#00a4ef' }}>Citas del Día</h3>
        <table className="modern-table">
          <thead>
            <tr>
              <th>ID Cita</th>
              <th>Paciente (Email)</th>
              <th>Servicio (ID)</th>
              <th>Estado Actual</th>
              <th>Acción (Cambiar Estado)</th>
            </tr>
          </thead>
          <tbody>
            {atenciones.length > 0 ? atenciones.map(a => (
              <tr key={a.id}>
                <td><strong>#{a.id}</strong></td>
                <td>{a.pacienteEmail}</td>
                <td>Servicio #{a.prestacionId}</td>
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
              </tr>
            )) : (
              <tr><td colSpan="5" style={{ textAlign: 'center', color: '#7f8c8d' }}>No hay citas registradas.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
