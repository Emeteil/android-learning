package com.example.android_project

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.round

class MainActivity : AppCompatActivity()
{
    private val calculatorEngine: CalculatorEngine = CalculatorEngine()
    private var currentExpression: String = ""
    private val LENGHT_LIMIT: Int = 15

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT // запретить поворот экрана

        findViewById<TextView>(R.id.button0).setOnClickListener({ AddToken("0") })
        findViewById<TextView>(R.id.button1).setOnClickListener({ AddToken("1") })
        findViewById<TextView>(R.id.button2).setOnClickListener({ AddToken("2") })
        findViewById<TextView>(R.id.button3).setOnClickListener({ AddToken("3") })
        findViewById<TextView>(R.id.button4).setOnClickListener({ AddToken("4") })
        findViewById<TextView>(R.id.button5).setOnClickListener({ AddToken("5") })
        findViewById<TextView>(R.id.button6).setOnClickListener({ AddToken("6") })
        findViewById<TextView>(R.id.button7).setOnClickListener({ AddToken("7") })
        findViewById<TextView>(R.id.button8).setOnClickListener({ AddToken("8") })
        findViewById<TextView>(R.id.button9).setOnClickListener({ AddToken("9") })
        findViewById<TextView>(R.id.buttonDecimal).setOnClickListener({ AddToken(".") })

        findViewById<TextView>(R.id.buttonSin).setOnClickListener({ AddToken("sin(") })
        findViewById<TextView>(R.id.buttonCos).setOnClickListener({ AddToken("cos(") })
        findViewById<TextView>(R.id.buttonLog).setOnClickListener({ AddToken("log(") })
        findViewById<TextView>(R.id.buttonPi).setOnClickListener({ AddToken("pi") })

        findViewById<TextView>(R.id.buttonAdd).setOnClickListener({ AddToken("+") })
        findViewById<TextView>(R.id.buttonSubtract).setOnClickListener({ AddToken("-") })
        findViewById<TextView>(R.id.buttonMultiply).setOnClickListener({ AddToken("*") })
        findViewById<TextView>(R.id.buttonDivide).setOnClickListener({ AddToken("/") })
        findViewById<TextView>(R.id.buttonPower).setOnClickListener({ AddToken("^") })

        findViewById<TextView>(R.id.buttonBracketOpen).setOnClickListener({ AddToken("(") })
        findViewById<TextView>(R.id.buttonBracketClose).setOnClickListener({ AddToken(")") })

        findViewById<TextView>(R.id.buttonEquals).setOnClickListener({ Equals() })
        findViewById<TextView>(R.id.buttonClear).setOnClickListener({ Clear() })
    }

    private fun UpdateInputField(text: String = currentExpression)
    {
        findViewById<TextView>(R.id.inputField).text = text.take(LENGHT_LIMIT)
    }

    private fun AddToken(token: String)
    {
        if (token == ".")
        {
            val lastNumber = currentExpression.takeLastWhile { it.isDigit() || it == '.' }
            if (lastNumber.contains('.')) return
        }
        if (token in arrayOf("+", "-", "*", "/", "^", "."))
        {
            if (currentExpression.isEmpty() && token != "-") return
            val operatorsAndFunctions = arrayOf("sin(", "cos(", "log(", "+", "-", "*", "/", "^")
            if (operatorsAndFunctions.any { currentExpression.endsWith(it) }) return
        }
        if (token == "(")
        {
            if (currentExpression.isNotEmpty())
            {
                val lastChar = currentExpression.last()
                if (lastChar !in arrayOf('+', '-', '*', '/', '^', '(')) return
            }
        }

        if (token == ")")
        {
            val openCount = currentExpression.count { it == '(' }
            val closeCount = currentExpression.count { it == ')' }
            val lastChar = if (currentExpression.isNotEmpty()) currentExpression.last() else ' '
            if (openCount <= closeCount || lastChar in arrayOf('+', '-', '*', '/', '^', '(')) return
        }

        currentExpression += token
        currentExpression = currentExpression.take(LENGHT_LIMIT)
        UpdateInputField()
    }

    private fun Clear()
    {
        if (currentExpression.isEmpty())
        {
            UpdateInputField("0")
            return
        }

        val funcs = arrayOf("sin(", "cos(", "log(")
        val constants = arrayOf("pi")

        if (funcs.any { currentExpression.endsWith(it) })
            currentExpression = currentExpression.dropLast(4)
        else if (constants.any { currentExpression.endsWith(it) })
            currentExpression = currentExpression.dropLast(2)
        else
            currentExpression = currentExpression.dropLast(1)

        UpdateInputField(currentExpression.ifEmpty { "0" })
    }

    private fun Equals()
    {
        if (currentExpression.isEmpty())
        {
            UpdateInputField("0")
            currentExpression = ""
            return
        }
        if (currentExpression.last() in arrayOf('+', '-', '*', '/', '^', '.')) return

        var result: Double;
        try
        {
            result = calculatorEngine.CalculateExpression(currentExpression)
        }
        catch (e: Exception)
        {
            UpdateInputField("Wrong")
            currentExpression = ""
            return
        }

        UpdateInputField((if (result % 1 <= 1e-15) result.toLong() else round(result * 10e15) / 10e15).toString())
        currentExpression = ""
    }
}