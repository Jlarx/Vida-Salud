import { useState, useEffect } from 'react';
import apiClient from '../apiClient';

export default function AdminDashboard() {
  const [prestaciones, setPrestaciones] = useState([]);
  const [newPrestacion, setNewPrestacion] = useState({ nombre: '', descripcion: '' });

  const fetchData = async () => {
    try {
      const res = await apiClient.get('/catalog/prestaciones');
      setPrestaciones(res.data);
    } catch (error) {
      console.error("Error fetching admin data", error);
      alert("Error al cargar datos: " + (error.response?.data?.message || error.message));
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreatePrestacion = async (e) => {
    e.preventDefault();
    try {
      await apiClient.post('/catalog/prestaciones', newPrestacion);
      setNewPrestacion({ nombre: '', descripcion: '' });
      fetchData();
    } catch (error) {
      console.error("Error creating prestacion", error);
      alert("Error al guardar: " + (error.response?.data?.message || error.message));
    }
  };

  return (
    <div>
      <h2 className="section-title">Catálogo de Servicios Médicos</h2>
      
      <div className="card-white" style={{ marginBottom: '2rem' }}>
        <h3 style={{ marginTop: 0, color: '#00a4ef' }}>Registrar Nuevo Servicio</h3>
        <form onSubmit={handleCreatePrestacion} className="clean-form">
          <input 
            type="text" 
            className="clean-input" 
            placeholder="Nombre (Ej. Consulta General)" 
            value={newPrestacion.nombre} 
            onChange={e => setNewPrestacion({...newPrestacion, nombre: e.target.value})} 
            required 
          />
          <input 
            type="text" 
            className="clean-input" 
            placeholder="Descripción" 
            value={newPrestacion.descripcion} 
            onChange={e => setNewPrestacion({...newPrestacion, descripcion: e.target.value})} 
            required 
          />
          <button type="submit" className="btn-action">Guardar Servicio</button>
        </form>
      </div>

      <div className="card-white">
        <h3 style={{ marginTop: 0, color: '#00a4ef' }}>Servicios Activos</h3>
        <table className="modern-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre del Servicio</th>
              <th>Descripción</th>
            </tr>
          </thead>
          <tbody>
            {prestaciones.length > 0 ? prestaciones.map(p => (
              <tr key={p.id}>
                <td><strong>#{p.id}</strong></td>
                <td style={{ fontWeight: 500 }}>{p.nombre}</td>
                <td>{p.descripcion}</td>
              </tr>
            )) : (
              <tr><td colSpan="3" style={{ textAlign: 'center', color: '#7f8c8d' }}>No hay servicios registrados</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
