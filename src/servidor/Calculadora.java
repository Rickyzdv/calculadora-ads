package servidor;

public class Calculadora {

    public static double sumar(double a, double b) {
        return a + b;
    }

    public static double restar(double a, double b) {
        return a - b;
    }

    public static double multiplicar(double a, double b) {
        return a * b;
    }

    public static double dividir(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("division entre cero no se puede");
        }
        return a / b;
    }

    public static double calcular(double a, String operador, double b) {
        switch (operador) {
            case "+":
                return sumar(a, b);
            case "-":
                return restar(a, b);
            case "*":
                return multiplicar(a, b);
            case "/":
                return dividir(a, b);
            default:
                throw new IllegalArgumentException("operador no valido: " + operador);
        }
    }

    // TEMPORAL: solo para probar. Se borra cuando conectemos el servidor.
    public static void main(String[] args) {
        System.out.println(calcular(5, "+", 3));
        System.out.println(calcular(5, "-", 3));
        System.out.println(calcular(5, "*", 3));
        System.out.println(calcular(5, "/", 3));
    }
}