package io.jettra.wui.components;

public class Inputs {
    public static class Password extends TextBox { public Password(String name) { super("password", name); } }
    public static class DatePicker extends TextBox { public DatePicker(String name) { super("date", name); } }
    public static class ColorPicker extends TextBox { public ColorPicker(String name) { super("color", name); } }
    public static class NumberField extends TextBox { public NumberField(String name) { super("number", name); } }
    public static class RangeSlider extends TextBox { public RangeSlider(String name) { super("range", name); } }
    public static class TimePicker extends TextBox { public TimePicker(String name) { super("time", name); } }
    public static class DateTimePicker extends TextBox { public DateTimePicker(String name) { super("datetime-local", name); } }
    public static class HiddenField extends TextBox { public HiddenField(String name) { super("hidden", name); } }
}
