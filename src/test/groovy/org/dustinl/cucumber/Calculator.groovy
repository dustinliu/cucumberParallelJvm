package org.dustinl.cucumber

public class Calculator {
    List<Double> stack = new ArrayList<Double>();

    public void push(double arg) {
        stack.add(arg);
    }

    public double divide() {
        return stack.get(0) / stack.get(1);
    }

    public double plus() {
        return stack.get(0) + stack.get(1);
    }

    public double minus() {
        return stack.get(0) - stack.get(1);
    }
}
