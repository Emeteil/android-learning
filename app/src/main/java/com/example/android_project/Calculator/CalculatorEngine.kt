package com.example.android_project.Calculator
import kotlin.math.*

data class Operator(val priority: Int, val operation: (Double, Double) -> Double)

class CalculatorEngine
{
    public val binaryOperators = mapOf(
        "+" to Operator(1, { a, b -> a + b }),
        "-" to Operator(1, { a, b -> a - b }),
        "*" to Operator(2, { a, b -> a * b }),
        "/" to Operator(2, { a, b -> a / b }),
        "^" to Operator(4, { a, b -> a.pow(b) })
    )

    public val unaryOperators = mapOf(
        "u+" to Operator(3, { a, _ -> a }),
        "u-" to Operator(3, { a, _ -> -a })
    )

    public val operators = binaryOperators + unaryOperators

    public val functions = mapOf<String, (Double) -> Double>(
        "sin" to { x -> sin(x) },
        "cos" to { x -> cos(x) },
        "log" to { x -> log10(x) }
    )

    public val constants = mapOf(
        "pi" to PI
    )

    public val operatorChars = binaryOperators.keys.joinToString("") + "()"

    public fun CalculateExpression(expression: String): Double
    {
        val tokens = Tokenize(expression)
        val rpn = ShuntingYard(tokens)
        return EvaluateRPN(rpn)
    }

    private fun Tokenize(expression: String): List<String>
    {
        if (expression.isEmpty()) throw Exception()

        val tokens = mutableListOf<String>()
        var i = 0
        var prevToken: String? = null

        while (i < expression.length)
        {
            val char = expression[i]

            if (char.isDigit() || char == '.')
            {
                var j = i
                while (j < expression.length && (expression[j].isDigit() || expression[j] == '.'))
                {
                    j++
                }
                val token = expression.substring(i, j)
                tokens.add(token)
                prevToken = token
                i = j
            }
            else if (char in operatorChars) // операторы и скобки
            {
                if (char == '+' || char == '-') // унарные операторы
                {
                    val isUnary = (
                        prevToken == null ||
                        prevToken in operators.keys || // после оператора
                        prevToken == "(" || // после открывающей скобки
                        prevToken in functions.keys // после функции
                    )

                    if (isUnary)
                        tokens.add(if (char == '+') "u+" else "u-")
                    else
                        tokens.add(char.toString())
                }
                else
                {
                    tokens.add(char.toString())
                }

                prevToken = char.toString()
                i++
            }
            else if (char.isLetter())
            {
                var j = i
                while (j < expression.length && (expression[j].isLetterOrDigit() || expression[j] == '_'))
                {
                    j++
                }
                val name = expression.substring(i, j)

                if (name in functions) // функции
                {
                    tokens.add(name)
                    prevToken = name
                }
                else if (name in constants) // константы
                {
                    tokens.add(constants[name].toString().replace(',', '.'))
                    prevToken = constants[name].toString().replace(',', '.')
                }
                else
                {
                    throw Exception()
                }
                i = j
            }
            else
            {
                throw Exception()
            }
        }

        return tokens
    }

    private fun ShuntingYard(tokens: List<String>): List<String>
    {
        // алгоритм сортировочной станции, самый правильный и просто способ для реализации приоритета операторов
        // output - для хранения итогового выражения
        // stack - для временного хранения операторов
        // возращает reverse Polish notation (RPN), эта нотации позволяет легко посчитать выражение по приоритету операторов в EvaluateRPN
        // https://en.wikipedia.org/wiki/Shunting_yard_algorithm
        // реализовывал по гайду https://habr.com/ru/articles/273253/
        val output = mutableListOf<String>()
        val stack = mutableListOf<String>()

        for (token in tokens)
        {
            if (IsNumber(token)) // число
            {
                output.add(token)
            }
            else if (token in functions) // функция
            {
                stack.add(token)
            }
            else if (token in operators) // оператор
            {
                while (stack.isNotEmpty() && stack.last() in operators && operators[stack.last()]!!.priority >= operators[token]!!.priority)
                {
                    output.add(stack.removeAt(stack.lastIndex))
                }
                stack.add(token)
            }
            else if (token == "(")
            {
                stack.add(token)
            }
            else if (token == ")")
            {
                // перекидываем в output пока не дойдём до (
                while (stack.isNotEmpty() && stack.last() != "(")
                {
                    output.add(stack.removeAt(stack.lastIndex))
                }

                if (stack.isEmpty()) throw Exception()
                

                stack.removeAt(stack.lastIndex) // убираем (

                if (stack.isNotEmpty() && stack.last() in functions)
                    output.add(stack.removeAt(stack.lastIndex))
            }
        }

        while (stack.isNotEmpty())
        {
            if (stack.last() == "(") throw Exception()
            
            output.add(stack.removeAt(stack.lastIndex))
        }

        return output
    }

    private fun IsNumber(token: String): Boolean
    {
        return try
        {
            val normalizedToken = token.replace(',', '.')
            normalizedToken.toDouble()
            true
        }
        catch (e: NumberFormatException)
        {
            false
        }
    }

    private fun EvaluateRPN(rpn: List<String>): Double
    {
        val stack = mutableListOf<Double>()

        for (token in rpn)
        {
            if (IsNumber(token))
            {
                val normalizedToken = token.replace(',', '.')
                stack.add(normalizedToken.toDouble())
                continue
            }
            if (token in operators)
            {
                if (token in unaryOperators) {
                    if (stack.isEmpty()) throw Exception()

                    val a = stack.removeAt(stack.lastIndex)
                    val result = operators[token]!!.operation(a, 0.0 /* второй аргумент отсутствует */)
                    stack.add(result)
                    continue
                }
                // иначе оператор бинарный
                if (stack.size < 2) throw Exception()

                val b = stack.removeAt(stack.lastIndex)
                val a = stack.removeAt(stack.lastIndex)
                val result = operators[token]!!.operation(a, b)
                stack.add(result)

                continue
            }
            if (token in functions)
            {
                if (stack.isEmpty()) throw Exception()

                val a = stack.removeAt(stack.lastIndex)
                val result = functions[token]!!(a)
                stack.add(result)
                continue
            }
        }

        if (stack.size != 1) throw Exception()
        
        return stack[0]
    }
}