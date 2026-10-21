package middleware;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MiddlewareMain {

    public static final int PUERTO = 5000;

    public static List<ManejadorConexion> clientes = new ArrayList<>();
    public static List<ManejadorConexion> servidores = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        ServerSocket puerta = new ServerSocket(PUERTO);
        System.out.println("Middleware escuchando en el puerto " + PUERTO);

        while (true) {
            Socket conexion = puerta.accept();
            System.out.println("Nueva conexion recibida, esperando identificacion...");

            ManejadorConexion manejador = new ManejadorConexion(conexion);
            new Thread(manejador).start();
        }
    }

    public static synchronized void registrarCliente(ManejadorConexion m) {
        clientes.add(m);
        System.out.println("Cliente registrado: " + m.getIdentificador()
                + " (clientes: " + clientes.size() + ", servidores: " + servidores.size() + ")");
    }

    public static synchronized void registrarServidor(ManejadorConexion m) {
        servidores.add(m);
        System.out.println("Servidor registrado: " + m.getIdentificador()
                + " (clientes: " + clientes.size() + ", servidores: " + servidores.size() + ")");
    }

    public static synchronized void eliminar(ManejadorConexion m) {
        clientes.remove(m);
        servidores.remove(m);
        System.out.println("Se desconecto: " + m.getIdentificador()
                + " (clientes: " + clientes.size() + ", servidores: " + servidores.size() + ")");
    }

    public static synchronized void enviarATodosLosServidores(String mensaje) {
        System.out.println("REENVIANDO A " + servidores.size() + " SERVIDOR(ES): " + mensaje);
        for (ManejadorConexion s : servidores) {
            s.enviar(mensaje);
        }
    }

    public static synchronized void enviarATodosLosClientes(String mensaje) {
        System.out.println("REENVIANDO A " + clientes.size() + " CLIENTE(S): " + mensaje);
        for (ManejadorConexion c : clientes) {
            c.enviar(mensaje);
        }
    }
}