package com.example.lepton.unocalculator;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    final String[] digits = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "."};
    final String[] operators = new String[]{"+", "-", "*", "/"};

    final Map<String, String[]> correspondingUnits = new HashMap<String, String[]>();
    final Map<String, double[]> correspondingConversions = new HashMap<String, double[]>();

    MathEval evaluator = new MathEval();

    SharedPreferences sharedpreferences;

    // Check if an element is in an array
    public boolean inArray(String[] array, String string) {

        for (String element : array) {
            if (element.replaceAll("\\s+", "").equals(string)) {
                return true;
            }
        }

        return false;
    }

    // Create an alert dialog based on a title and message
    public void alertDialog(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(Html.fromHtml(message))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Delete the window
                    }
                }).show();
    }

    // Find the category of a specific unit
    public String findCategory(Map<String, String[]> mapping, String unit) {
        for (Map.Entry<String, String[]> entry : mapping.entrySet()) {
            if (inArray(entry.getValue(), unit)) {
                return entry.getKey();
            }
        }

        return "";
    }

    // Finds the index of a string within an array
    public int findIndex(String[] array, String string) {
        int index = 0;

        for (String element : array) {
            if (element.equals(string)) {
                return index;
            }

            index++;
        }

        return -1;
    }

    // Converts an array of strings into a combined string
    public String combineStrings(String[] strings) {
        String returnString = "";

        for (String string : strings) {
            returnString += string;
        }

        return returnString;
    }

    // Takes in a unitary expression, outputs a numerical expression
    public String replaceInExpression(String[] array, String target, String replacement) {
        String expression = "";

        for (int i = 0; i < array.length; i++) {
            String string = array[i];

            if (string.contains(target)) {
                String[] components = string.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

                for (int j = 0; j < components.length; j++) {
                    if (components[j].equals(target)) {
                        components[j] = replacement;

                        String[] arrayComponents = array[i].split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
                        arrayComponents[1] = components[j];
                        array[i] = combineStrings(arrayComponents);

                    } else if (components[j].contains(target)) {
                        components[j] = "*1";
                    }
                }

                for (String component : components) {
                    expression += component;
                }
            } else {
                expression += string;
            }
        }

        return expression;
    }

    // Check if string contains digit
    public static boolean containsDigit(String s) {
        if (s != null && !s.isEmpty()) {
            for (char c : s.toCharArray()) {
                if (Character.isDigit(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Solves an expression
    public void solveExpression(EditText editText) {
        // Redo syntax highlighting
        String[] lines = editText.getText().toString().split("\\r?\\n");
        String string = lines[lines.length - 1];
        String expression = "";

        // Check if string is empty or not
        if (string.isEmpty() || !containsDigit(string)) {
            alertDialog("Input Error", "You haven't inputted an expression.");
        }
        // Calculate if edge case is not satisfied
        else {
            try {
                String[] unitTokens = string.replaceAll("[0-9,+,-,*,/]", " ").split("\\s+");

                // Insert whitespace between constants and operations
                for (String operator : operators) {
                    string = string.replace(operator, " " + operator + " ");
                }

                String[] words = string.split("\\s+");

                Set<String> units = new HashSet<>();
                String conversionUnit = "";

                for (String word : unitTokens) {
                    if (!word.isEmpty() & !word.equals(".")) {
                        for (String operator : operators) {
                            word = word.replace("-", "");
                        }

                        units.add(word);
                        conversionUnit = word;
                    }
                }

                if (units.size() == 0) {
                    expression = string;
                } else if (units.size() == 1) {
                    expression = string.replaceAll(conversionUnit, "*1");
                } else {
                    String category = findCategory(correspondingUnits, conversionUnit);

                    for (String unit : units) {
                        if (!unit.equals(conversionUnit)) {
                            expression = replaceInExpression(words, unit, "*" +
                                    correspondingConversions.get(unit)[findIndex(correspondingUnits.get(category), conversionUnit)]);
                        }
                    }

                    expression = expression.replaceAll(conversionUnit, "*1");
                }

                expression = expression.replaceAll("E", "*10^");
                expression = expression.replaceAll("e", "*10^");
                double result = (double) Math.round(evaluator.evaluate(expression) * 100000d) / 100000d;
                editText.setText(editText.getText().toString() + "\n" + result + conversionUnit + "\n");

                editText.setSelection(editText.getText().length());

                // Syntax highlighting
                for (String digit : digits) {
                    fullSyntaxHighlight(digit, editText.getText(), new int[]{216, 41, 41});
                }

                for (String operator : operators) {
                    fullSyntaxHighlight(operator, editText.getText(), new int[]{16, 188, 25});
                }

                // Store data in SharedPreferences
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("Text", editText.getText().toString());
                editor.commit();
            } catch (Exception e) {
                System.out.println("Error: " + e.toString());
                alertDialog("Invalid Input", "Please sure you have inputted a valid expression. Check the guide for reference.");
            }
        }
    }

    // Compute button press
    public void compute(View v) {
        solveExpression((EditText) findViewById(R.id.editText));
    }

    // Complete / full syntax highlighting
    public void fullSyntaxHighlight(String search, Editable s, int[] rgb) {
        String text = s.toString();
        int index = text.indexOf(search);

        while (index != -1) {
            s.setSpan(
                    new ForegroundColorSpan(Color.rgb(rgb[0], rgb[1], rgb[2])),
                    index,
                    index + search.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            index = text.indexOf(search, index + 1);
        }
    }

    // Syntax highlighting based on search term
    public void syntaxHighlight(String search, Editable s, int[] rgb) {
        int index = s.toString().lastIndexOf(search);
        if (index >= 0) {
            s.setSpan(
                    new ForegroundColorSpan(Color.rgb(rgb[0], rgb[1], rgb[2])),
                    index,
                    index + search.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    // Clear all of the text
    public void clear(View v) {
        EditText editText = (EditText) findViewById(R.id.editText);
        editText.setText("");
    }

    // Delete the current line of text
    public void delete(View v) {
        EditText editText = (EditText) findViewById(R.id.editText);
        String[] lines = editText.getText().toString().split("\n");
        String newText = "";

        for (int i = 0; i < lines.length - 1; i++) {
            newText += lines[i];
            newText += "\n";
        }

        editText.setText(newText);
        editText.setSelection(editText.getText().length());

        // Syntax highlighting
        for (String digit : digits) {
            fullSyntaxHighlight(digit, editText.getText(), new int[]{216, 41, 41});
        }

        for (String operator : operators) {
            fullSyntaxHighlight(operator, editText.getText(), new int[]{16, 188, 25});
        }
    }

    // Create the Compute guide for users to reference syntax
    public void showGuide(View v) {
        alertDialog("User Guide", "Unitary expressions are parsed like any other mathematical expression," +
                " except with units at the end of coefficients. Units must" +
                " be in the same category (i.e. you cannot use oz and yd in an expression), and the conversion unit is the <b>last unit</b>. Ex: <br><br><b>1cm + 3m + 1km</b>");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedpreferences = getSharedPreferences("Text", Context.MODE_PRIVATE);
        String text = sharedpreferences.getString("Text", "");

        // Syntax highlighting
        final EditText editText = (EditText) findViewById(R.id.editText);

        try {
            editText.setText(text);

            // Syntax highlighting
            for (String digit : digits) {
                fullSyntaxHighlight(digit, editText.getText(), new int[]{216, 41, 41});
            }

            for (String operator : operators) {
                fullSyntaxHighlight(operator, editText.getText(), new int[]{16, 188, 25});
            }
        }
        catch (Exception e) {
            System.out.println("Exception: " + e.toString());
        }

        // Put edittext in focus
        editText.setFocusableInTouchMode(true);
        editText.requestFocus();

        // All possible units
        correspondingUnits.put("Length", new String[]{"in", "ft", "yd", "m", "cm", "mm", "km", "mi"});
        correspondingUnits.put("Weight", new String[]{"gr", "oz", "lb", "g", "kg", "mg", "ton", "tons", "st"});
        correspondingUnits.put("Money", new String[]{"USD", "EUR", "JPY", "GBP", "AUD", "CHF", "CAD", "CNY"});
        correspondingUnits.put("Time", new String[]{"ms", "sec", "min", "hr", "day", "days", "wk", "mon", "yr", "yrs"});
        correspondingUnits.put("Angle", new String[]{"deg", "rad"});

        // Length conversions
        correspondingConversions.put("in", new double[]{1, 0.0833d, 0.02778d, 0.0254d, 2.54d, 25.4d, 2.54e-5, 1.57828e-5});
        correspondingConversions.put("ft", new double[]{12, 1, 0.333d, 0.0348d, 30.48d, 304.8d, 0.0003048d, 0.00018939d});
        correspondingConversions.put("yd", new double[]{36, 3, 1, 0.9144d, 91.44d, 914.4d, 0.0009144d, 0.000568182d});
        correspondingConversions.put("m", new double[]{39.3701d, 3.28084d, 1.09361d, 1, 100, 1000, 0.001d, 0.000621371d});
        correspondingConversions.put("cm", new double[]{0.393701d, 0.0328084d, 0.0109361d, 0.01d, 1, 10, 1e-5d, 6.21371e-6d});
        correspondingConversions.put("mm", new double[]{0.0393701d, 0.00328084d, 0.00109361d, 0.001d, 0.1, 1, 1e-6d, 6.21371e-7});
        correspondingConversions.put("km", new double[]{39370.1d, 3280.84d, 1093.61d, 1000, 100000, 1000000, 1, 0.621371d});
        correspondingConversions.put("mi", new double[]{63360, 5280, 1760, 1609.344d, 160934.4d, 1609344, 1.609344d, 1});

        // Weight
        correspondingConversions.put("gr", new double[]{1, 0.002286d, 0.000143d, 0.064799d, 6.47989e-5d, 64.79891d, 6.4799e-8d, 6.4799e-8d, 0.00001});
        correspondingConversions.put("oz", new double[]{437.5d, 1, 0.0625d, 28.3495d, 0.0283495d, 28349.5231d, 0.000028d, 0.000028d, 0.004464d});
        correspondingConversions.put("lb", new double[]{7000, 16, 1, 453.59237d, 0.453592d, 453592.37d, 0.000454d, 0.000454d, 0.071429d});
        correspondingConversions.put("g", new double[]{15.432358d, 0.035274d, 0.002205d, 1, 0.001d, 1000, 0.000001d, 0.000001d, 0.000157d});
        correspondingConversions.put("kg", new double[]{15432.4d, 35.274d, 2.20462d, 1000, 1, 1000000, 0.00110231d, 0.00110231d, 0.157473d});
        correspondingConversions.put("mg", new double[]{0.015432, 3.53E-05, 2.20E-06, 0.01, 1.00E-06, 1, 1.10E-09, 1.10E-9, 1.57E-07});
        correspondingConversions.put("ton", new double[]{1.40E+07, 32000, 2000, 907185, 907.185, 9.07E+08, 1, 1, 142.857});
        correspondingConversions.put("tons", new double[]{1.40E+07, 32000, 2000, 907185, 907.185, 9.07E+08, 1, 1, 142.857});
        correspondingConversions.put("st", new double[]{98000, 224, 14, 6530.29, 6.35029, 6.35E+6, 0.007, 0.007, 1});

        // Time
        correspondingConversions.put("ms", new double[]{1, 0.001, 0.000017, 2.7778E-7, 1.1574E-8, 1.1574E-8, 1.6534E-9, 3.8052E-10, 3.171E-11, 3.171E-11});
        correspondingConversions.put("sec", new double[]{1000, 1, 0.016667d, 0.000278d, 0.000012d, 0.00012d, 0.000002d, 3.8052E-7, 3.171E-8d, 3.171E-8});
        correspondingConversions.put("min", new double[]{60000, 60, 1, 0.01667d, 0.000694d, 0.000694d, 0.000099d, 0.000023d, 0.000002d, 0.000002d});
        correspondingConversions.put("hr", new double[]{3600000, 3600, 60, 1, 0.041667d, 0.041667d, 0.005952, 0.00137, 0.000114d, 0.000114d});
        correspondingConversions.put("day", new double[]{86400000, 86400, 1440, 24, 1, 1, 0.0142857d, 0.032877d, 0.00274, 0.00274});
        correspondingConversions.put("days", new double[]{86400000, 86400, 1440, 24, 1, 1, 0.0142857d, 0.032877d, 0.00274, 0.00274});
        correspondingConversions.put("wk", new double[]{604800000, 604800, 10080, 168, 7, 7, 1, 0.0230137d, 0.19178d, 0.19178d});
        correspondingConversions.put("mon", new double[]{2628000000d, 2628000, 43800, 730, 30.41667d, 30.41667d, 4.4345238d, 1, 0.083333d, 0.083333d});
        correspondingConversions.put("yr", new double[]{3.1540E+10d, 31536000, 525600, 8760, 365, 365, 52.142857, 12, 1, 1});
        correspondingConversions.put("yrs", new double[]{3.1540E+10d, 31536000, 525600, 8760, 365, 365, 52.142857, 12, 1, 1});

        // Angle
        correspondingConversions.put("deg", new double[]{1, Math.PI / 180});
        correspondingConversions.put("rad", new double[]{180 / Math.PI, 1});


        editText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    solveExpression(editText);
                    return true;
                }
                return false;
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                for (String digit : digits) {
                    syntaxHighlight(digit, s, new int[] {216, 41, 41});
                }

                for (String operator : operators) {
                    syntaxHighlight(operator, s, new int[] {16, 188, 25});
                }
            }
        });
    }
}