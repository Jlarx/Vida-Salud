import { useState, useEffect } from 'react';
import apiClient from '../apiClient';

export default function AdminDashboard() {
  const [prestaciones, setPrestaciones] = useState([]);
  const [newPrestacion, setNewPrestacion] = useState({ nombre: '', descripcion: '' });
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState({ nombre: '', descripcion: '' });

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

  const handleDelete = async (id) => {
    if (!window.confirm("¿Estás seguro de eliminar este servicio?")) return;
    try {
      await apiClient.delete(`/catalog/prestaciones/${id}`);
      fetchData();
    } catch (error) {
      alert("Error al eliminar: " + (error.response?.data?.message || error.message));
    }
  };

  const handleEditClick = (p) => {
    setEditingId(p.id);
    setEditForm({ nombre: p.nombre, descripcion: p.descripcion });
  };

  const handleUpdate = async (id) => {
    try {
      await apiClient.put(`/catalog/prestaciones/${id}`, editForm);
      setEditingId(null);
      fetchData();
    } catch (error) {
      alert("Error al actualizar: " + (error.response?.data?.message || error.message));
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
              <th style={{ textAlign: 'right' }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {prestaciones.length > 0 ? prestaciones.map(p => (
              <tr key={p.id}>
                <td><strong>#{p.id}</strong></td>
                <td style={{ fontWeight: 500 }}>
                  {editingId === p.id ? (
                    <input type="text" className="clean-input" style={{ margin: 0 }} value={editForm.nombre} onChange={e => setEditForm({...editForm, nombre: e.target.value})} />
                  ) : p.nombre}
                </td>
                <td>
                  {editingId === p.id ? (
                    <input type="text" className="clean-input" style={{ margin: 0 }} value={editForm.descripcion} onChange={e => setEditForm({...editForm, descripcion: e.target.value})} />
                  ) : p.descripcion}
                </td>
                <td style={{ textAlign: 'right' }}>
                  {editingId === p.id ? (
                    <>
                      <button className="btn-action" style={{ background: '#7fba00', padding: '5px 10px', fontSize: '12px', marginRight: '5px' }} onClick={() => handleUpdate(p.id)}>Guardar</button>
                      <button className="btn-action" style={{ background: '#666', padding: '5px 10px', fontSize: '12px' }} onClick={() => setEditingId(null)}>Cancelar</button>
                    </>
                  ) : (
                    <>
                      <button className="btn-action" style={{ background: '#ffb900', color: '#333', padding: '5px 10px', fontSize: '12px', marginRight: '5px' }} onClick={() => handleEditClick(p)}>Editar</button>
                      <button className="btn-action" style={{ background: '#f25022', padding: '5px 10px', fontSize: '12px' }} onClick={() => handleDelete(p.id)}>Eliminar</button>
                    </>
                  )}
                </td>
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
