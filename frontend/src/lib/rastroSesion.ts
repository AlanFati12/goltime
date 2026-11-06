const CLAVE = 'goltime:sesion'

/**
 * La cookie de renovación es HttpOnly: el JavaScript no puede saber si existe. Para no pedir la renovación (y
 * dejar toda la app esperando una petición) a cada visitante anónimo, al iniciar sesión se deja un aviso en el
 * navegador y al cerrarla se borra. El aviso no es un secreto ni da acceso a nada: solo dice «vale la pena
 * intentar renovar». Si el almacenamiento no está disponible (modo privado estricto), se intenta renovar siempre.
 */
export function hayRastroDeSesion(): boolean {
  try {
    return localStorage.getItem(CLAVE) === '1'
  } catch {
    return true
  }
}

export function dejarRastroDeSesion(): void {
  try {
    localStorage.setItem(CLAVE, '1')
  } catch {
    /* sin almacenamiento: no pasa nada */
  }
}

export function borrarRastroDeSesion(): void {
  try {
    localStorage.removeItem(CLAVE)
  } catch {
    /* sin almacenamiento: no pasa nada */
  }
}
