import type { Usuario } from '@/caracteristicas/autenticacion/almacenSesion'

/** Usuario completo para pruebas; se puede sobreescribir cualquier campo. */
export function usuarioDePrueba(cambios: Partial<Usuario> = {}): Usuario {
  return {
    id: 1,
    nombres: 'Ana',
    apellidos: 'Pérez',
    correo: 'ana@correo.com',
    telefono: null,
    documento: null,
    rol: 'CLIENTE',
    activo: true,
    correoVerificado: true,
    telefonoVerificado: false,
    motivoDesactivacion: null,
    fechaDesactivacion: null,
    puedeInscribirEquipos: false,
    ultimoLogin: null,
    creadoEn: '2026-10-18T00:00:00Z',
    ...cambios,
  }
}
