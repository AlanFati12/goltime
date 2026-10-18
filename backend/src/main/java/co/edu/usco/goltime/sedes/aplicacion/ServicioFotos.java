package co.edu.usco.goltime.sedes.aplicacion;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.aplicacion.AlmacenArchivos;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.TipoImagen;
import co.edu.usco.goltime.sedes.dominio.Foto;

/** Fotos de sedes y canchas: valida tipo (por firma del archivo), tamaño y cantidad. */
@Service
public class ServicioFotos {

    public static final int TAMANO_MAXIMO_BYTES = 5 * 1024 * 1024;
    public static final int FOTOS_MAXIMAS = 10;

    private final RepositorioSedes sedes;
    private final RepositorioCanchas canchas;
    private final AlmacenArchivos almacen;
    private final RegistroAuditoria auditoria;

    public ServicioFotos(RepositorioSedes sedes, RepositorioCanchas canchas, AlmacenArchivos almacen,
            RegistroAuditoria auditoria) {
        this.sedes = sedes;
        this.canchas = canchas;
        this.almacen = almacen;
        this.auditoria = auditoria;
    }

    @Transactional
    public Foto agregarASede(long sedeId, byte[] contenido) {
        sedes.buscarPorId(sedeId).orElseThrow(() -> noExiste("La sede no existe."));
        validar(contenido, sedes.fotos(sedeId).size());
        String archivo = almacen.guardar(contenido, TipoImagen.detectar(contenido).orElseThrow().extension());
        Foto foto = sedes.agregarFoto(sedeId, archivo);
        auditoria.registrar("AGREGAR_FOTO_SEDE", "sedes", sedeId, Map.of("fotoId", foto.id()), null);
        return foto;
    }

    @Transactional
    public Foto agregarACancha(long canchaId, byte[] contenido) {
        canchas.buscarPorId(canchaId).orElseThrow(() -> noExiste("La cancha no existe."));
        validar(contenido, canchas.fotos(canchaId).size());
        String archivo = almacen.guardar(contenido, TipoImagen.detectar(contenido).orElseThrow().extension());
        Foto foto = canchas.agregarFoto(canchaId, archivo);
        auditoria.registrar("AGREGAR_FOTO_CANCHA", "canchas", canchaId, Map.of("fotoId", foto.id()), null);
        return foto;
    }

    @Transactional
    public void quitarDeSede(long sedeId, long fotoId) {
        Foto foto = sedes.quitarFoto(sedeId, fotoId).orElseThrow(() -> noExiste("La foto no existe."));
        almacen.borrar(foto.archivo());
        auditoria.registrar("QUITAR_FOTO_SEDE", "sedes", sedeId, Map.of("fotoId", fotoId), null);
    }

    @Transactional
    public void quitarDeCancha(long canchaId, long fotoId) {
        Foto foto = canchas.quitarFoto(canchaId, fotoId).orElseThrow(() -> noExiste("La foto no existe."));
        almacen.borrar(foto.archivo());
        auditoria.registrar("QUITAR_FOTO_CANCHA", "canchas", canchaId, Map.of("fotoId", fotoId), null);
    }

    private static void validar(byte[] contenido, int existentes) {
        if (contenido == null || contenido.length == 0) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El archivo está vacío.");
        }
        if (contenido.length > TAMANO_MAXIMO_BYTES) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La imagen supera el tamaño máximo de 5 MB.");
        }
        if (TipoImagen.detectar(contenido).isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Solo se aceptan imágenes JPG, PNG o WebP.");
        }
        if (existentes >= FOTOS_MAXIMAS) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Se alcanzó el máximo de " + FOTOS_MAXIMAS + " fotos.");
        }
    }

    private static ExcepcionNegocio noExiste(String mensaje) {
        return new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, mensaje);
    }
}
