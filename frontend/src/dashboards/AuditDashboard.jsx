import { useState } from 'react';
import apiClient from '../apiClient';

export default function AuditDashboard() {
  const [atencionId, setAtencionId] = useState('');
  const [timeline, setTimeline] = useState([]);

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!atencionId) return;
    
    try {
      const res = await apiClient.get(`/audit/timeline/${atencionId}`);
      setTimeline(res.data);
    } catch (error) {
      console.error("Error fetching audit timeline", error);
    }
  };

  return (
    <div>
      <h2 className="section-title">Auditoría Médica</h2>
      
      <div className="card-white" style={{ marginBottom: '2rem' }}>
        <h3 style={{ marginTop: 0, color: '#00a4ef' }}>Rastrear Cambios de una Cita</h3>
        <form onSubmit={handleSearch} className="clean-form">
          <input 
            type="number" 
            className="clean-input"
            placeholder="Ingrese el ID de la Cita (Ej. 1)" 
            value={atencionId} 
            onChange={e => setAtencionId(e.target.value)} 
            required 
          />
          <button type="submit" className="btn-action">Buscar Historial</button>
        </form>
      </div>

      <div className="card-white">
        <h3 style={{ marginTop: 0, color: '#00a4ef' }}>Línea de Tiempo</h3>
        <table className="modern-table">
          <thead>
            <tr>
              <th>ID Auditoría</th>
              <th>Autor del Cambio (Email)</th>
              <th>Cambio de Estado</th>
              <th>Fecha y Hora</th>
            </tr>
          </thead>
          <tbody>
            {timeline.length > 0 ? timeline.map(log => (
              <tr key={log.id}>
                <td><strong>#{log.id}</strong></td>
                <td>{log.usuarioEmail}</td>
                <td>
                  <span className={`status-pill status-${log.estadoAnterior}`}>{log.estadoAnterior}</span>
                  <span style={{ margin: '0 10px', color: '#ccc' }}>➔</span>
                  <span className={`status-pill status-${log.estadoNuevo}`}>{log.estadoNuevo}</span>
                </td>
                <td>{new Date(log.fechaCambio).toLocaleString()}</td>
              </tr>
            )) : (
              <tr><td colSpan="4" style={{ textAlign: 'center', color: '#7f8c8d' }}>Realice una búsqueda para ver el historial.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
