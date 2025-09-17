import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class Point(_x: Float, _y: Float)
{
    var x: Float = _x
        set(value)
        {
            field = value
        }
    var y: Float = _y
        set(value)
        {
            field = value
        }

    fun AddVector2(vector: Vector2, multiplier: Float)
    {
        this.x += vector.x * multiplier
        this.y += vector.y * multiplier
    }

    fun ToString(): String
    {
        return "(${x}, ${y})"
    }
}

// нормализованный
class Vector2(_x: Float, _y: Float)
{
    var x: Float = 0f
    var y: Float = 0f

    init
    {
        var d = sqrt(_x.pow(2) + y.pow(2))
        this.x = _x / d
        this.y = _y / d
    }

    fun ToString(): String
    {
        return "(${x}, ${y})"
    }
}

class Human(fullName: String, age: Int, groupNumber: Int, startSpeed: Float)
{
    var fullName: String = ""
    var age: Int = 0
        set(value)
        {
            if (value > 0)
                field = value
        }
    var groupNumber: Int = 0
        set(value)
        {
            if (value > 0)
                field = value
        }

    var currentSpeed: Float = 0.0f
        get() { return field }
        private set(value) { field = value }
    var currentPosition: Point = Point(0f, 0f)
        get() { return field }
        private set(value) { field = value }
    var currentMoveVector2: Vector2 = Vector2(0f, 0f)
        get() { return field }
        private set(value) { field = value }

    init
    {
        this.fullName = fullName
        if (age > 0)
            this.age = age
        this.groupNumber = groupNumber
        if (groupNumber > 0)
            this.groupNumber = groupNumber

        this.currentSpeed = startSpeed
        RandomizeMoveVector()
    }

    fun RandomizeMoveVector()
    {
        currentMoveVector2.x = Random.nextFloat() * 2 - 1
        currentMoveVector2.y = Random.nextFloat() * 2 - 1
    }

    fun Move()
    {
        currentPosition.AddVector2(currentMoveVector2, currentSpeed)
    }

    fun Print()
    {
        println("${fullName}: ${currentPosition.ToString()}")
    }
}

fun main() {
    var peoples: Array<Human> = arrayOf(
        Human("Я", 19, 433, 1f),
        Human("Петя", 20, 123, 1f),
        Human("Ваня", 20, 867, 1f),
    )
    while (true)
    {
        for (people in peoples)
        {
            people.RandomizeMoveVector()
            people.Move()
            people.Print()
        }
        readLine()
    }
}