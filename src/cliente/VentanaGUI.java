package cliente;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class VentanaGUI extends JFrame {

    private JLabel pantalla = new JLabel("0");
    private JLabel etiquetaEstado = new JLabel(" ");
    private JTextArea area = new JTextArea(10, 30);

    private String primerNumero = "";
    private String segundoNumero = "";
    private String operador = "";

    private static final Color FONDO = new Color(245, 245, 245);
    private static final Color GRIS_NUMERO = new Color(250, 250, 250);
    private static final Color NARANJA = new Color(255, 149, 0);
    private static final Color GRIS_ACCION = new Color(210, 210, 210);

    public VentanaGUI(String titulo) {
        setTitle("Calculadora distribuida - " + titulo);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 8));
        getContentPane().setBackground(FONDO);

        // ---------- PANTALLA ----------
        JPanel panelPantalla = new JPanel(new BorderLayout());
        panelPantalla.setBackground(Color.WHITE);
        panelPantalla.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        pantalla.setFont(new Font("Segoe UI", Font.BOLD, 34));
        pantalla.setHorizontalAlignment(JLabel.RIGHT);

        etiquetaEstado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        etiquetaEstado.setForeground(new Color(130, 130, 130));
        etiquetaEstado.setHorizontalAlignment(JLabel.RIGHT);

        panelPantalla.add(etiquetaEstado, BorderLayout.NORTH);
        panelPantalla.add(pantalla, BorderLayout.CENTER);
        add(panelPantalla, BorderLayout.NORTH);

        // ---------- BOTONES ----------
        JPanel panelBotones = new JPanel(new GridLayout(5, 4, 6, 6));
        panelBotones.setBackground(FONDO);
        panelBotones.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        String[] etiquetas = {
                "C", "", "", "/",
                "7", "8", "9", "*",
                "4", "5", "6", "-",
                "1", "2", "3", "+",
                "0", ".", "", "="
        };

        for (String etiqueta : etiquetas) {
            if (etiqueta.equals("")) {
                JPanel vacio = new JPanel();
                vacio.setBackground(FONDO);
                panelBotones.add(vacio);
                continue;
            }
            panelBotones.add(crearBoton(etiqueta));
        }

        add(panelBotones, BorderLayout.CENTER);

        // ---------- BITACORA ----------
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(new Color(252, 252, 252));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(380, 190));
        scroll.setBorder(BorderFactory.createTitledBorder("Mensajes recibidos del middleware"));
        add(scroll, BorderLayout.SOUTH);

        setSize(420, 640);
        setLocationRelativeTo(null);
    }

    private JButton crearBoton(String etiqueta) {
        JButton boton = new JButton(etiqueta);
        boton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createLineBorder(new Color(215, 215, 215)));

        if (etiqueta.equals("+") || etiqueta.equals("-")
                || etiqueta.equals("*") || etiqueta.equals("/") || etiqueta.equals("=")) {
            boton.setBackground(NARANJA);
            boton.setForeground(Color.WHITE);
        } else if (etiqueta.equals("C")) {
            boton.setBackground(GRIS_ACCION);
        } else {
            boton.setBackground(GRIS_NUMERO);
        }

        boton.setOpaque(true);
        boton.addActionListener(e -> presionar(etiqueta));
        return boton;
    }

    private void presionar(String tecla) {

        if (tecla.equals("C")) {
            primerNumero = "";
            segundoNumero = "";
            operador = "";
            pantalla.setText("0");
            etiquetaEstado.setText(" ");
            return;
        }

        if (tecla.equals("+") || tecla.equals("-") || tecla.equals("*") || tecla.equals("/")) {
            if (primerNumero.equals("")) {
                return;
            }
            operador = tecla;
            etiquetaEstado.setText(primerNumero + " " + operador);
            pantalla.setText("0");
            return;
        }

        if (tecla.equals("=")) {
            if (primerNumero.equals("") || operador.equals("") || segundoNumero.equals("")) {
                mostrarLinea("Falta completar la operacion.");
                return;
            }
            try {
                double a = Double.parseDouble(primerNumero);
                double b = Double.parseDouble(segundoNumero);
                etiquetaEstado.setText(primerNumero + " " + operador + " " + segundoNumero + " =");
                pantalla.setText("esperando...");
                ClienteMain.enviarOperacion(a, operador, b);
            } catch (NumberFormatException ex) {
                mostrarLinea("Numero invalido.");
            }
            primerNumero = "";
            segundoNumero = "";
            operador = "";
            return;
        }

        if (operador.equals("")) {
            primerNumero = primerNumero + tecla;
            pantalla.setText(primerNumero);
        } else {
            segundoNumero = segundoNumero + tecla;
            pantalla.setText(segundoNumero);
        }
    }

    // Se llama desde ClienteMain cuando llega un resultado propio
    public void mostrarResultado(String resultado) {
        SwingUtilities.invokeLater(() -> pantalla.setText(resultado));
    }

    public void mostrarLinea(String texto) {
        SwingUtilities.invokeLater(() -> {
            area.append(texto + "\n");
            area.setCaretPosition(area.getDocument().getLength());
        });
    }
}