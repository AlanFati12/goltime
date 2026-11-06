package co.edu.usco.goltime.compartido;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Cuenta las sentencias SQL que se ejecutan contra la base. Sirve para vigilar el problema de las consultas
 * N+1: una petición cuyo número de sentencias crece con la cantidad de filas devueltas. Se activa con
 * {@code @Import(ContadorSentencias.class)} en la prueba que lo necesite (crea su propio contexto de Spring).
 */
@TestConfiguration
public class ContadorSentencias {

    private static final AtomicLong TOTAL = new AtomicLong();
    private static final Set<String> EJECUCIONES = Set.of("execute", "executeQuery", "executeUpdate", "executeLargeUpdate", "executeBatch");

    /** Sentencias ejecutadas hasta ahora; la diferencia entre dos lecturas es lo que costó lo del medio. */
    public static long total() {
        return TOTAL.get();
    }

    @Bean
    static BeanPostProcessor envolverDataSource() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nombre) {
                if (!(bean instanceof DataSource origen)) {
                    return bean;
                }
                return Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class<?>[] { DataSource.class }, (p, metodo, args) -> {
                    Object r = invocar(origen, metodo, args);
                    return r instanceof Connection c ? envolverConexion(c) : r;
                });
            }
        };
    }

    private static Connection envolverConexion(Connection conexion) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class }, (p, metodo, args) -> {
            Object r = invocar(conexion, metodo, args);
            if (r instanceof Statement s && (metodo.getName().equals("prepareStatement") || metodo.getName().equals("createStatement") || metodo.getName().equals("prepareCall"))) {
                return envolverSentencia(s, metodo.getReturnType());
            }
            return r;
        });
    }

    private static Object envolverSentencia(Statement sentencia, Class<?> tipo) {
        return Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] { tipo }, (p, metodo, args) -> {
            if (EJECUCIONES.contains(metodo.getName())) {
                TOTAL.incrementAndGet();
            }
            return invocar(sentencia, metodo, args);
        });
    }

    private static Object invocar(Object destino, java.lang.reflect.Method metodo, Object[] args) throws Throwable {
        try {
            return metodo.invoke(destino, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
