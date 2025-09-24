import java.util.concurrent.locks.ReentrantLock
// мьютекс из java (kotlinx.coroutines.sync.Mutex для корутин котлина, я буду использовать java потоки. Как будто так привычней, без suspend функций)

import kotlin.random.Random

open class Human(fullName: String, age: Int, groupNumber: Int, startSpeed: Float) : Movable
{
    val mutex: ReentrantLock = ReentrantLock() // для каждого экземпляра будет свой мьютекс

    var fullName: String = ""
        set(value)
        {
            mutex.lock() // для сеттеров буду использовать мьютекс. Для всех полей - один мьютекс (раньше я добавлял мьютекс лок в методы, но так как будто удобнее)
            try // try except для того, чтобы мьютек не заблокировался навсегда в случае ошибки
            {
                field = value
            }
            finally
            {
                mutex.unlock()
            }
        }
    var age: Int = 0
        set(value) // тут сокращённая записась, для читаймости
        {
            mutex.lock()
            try { if (value > 0) field = value }
            finally { mutex.unlock() }
        }
    var groupNumber: Int = 0
        set(value)
        {
            mutex.lock()
            try { if (value > 0) field = value }
            finally { mutex.unlock() }
        }

    override var currentSpeed: Float = 0.0f
        get() { return field }
        protected set(value)
        {
            mutex.lock()
            try { field = value }
            finally { mutex.unlock() }
        }
    override var currentPosition: Point = Point(0f, 0f)
        get() { return field }
        protected set(value)
        {
            mutex.lock()
            try { field = value }
            finally { mutex.unlock() }
        }
    override var currentMoveVector2: Vector2 = Vector2(0f, 0f)
        get() { return field }
        protected set(value)
        {
            mutex.lock()
            try { field = value }
            finally { mutex.unlock() }
        }

    init
    {
        this.fullName = fullName
        if (age > 0)
            this.age = age
        this.groupNumber = groupNumber
        if (groupNumber > 0)
            this.groupNumber = groupNumber

        this.currentSpeed = startSpeed
    }

    override fun RandomizeMoveVector()
    {
        // тут мьютекс необязателен, я его убрал
        currentMoveVector2.x = Random.nextFloat() * 2 - 1
        currentMoveVector2.y = Random.nextFloat() * 2 - 1
    }

    override fun Move()
    {
        // тут тоже необязателенм, я сделал классы Point и Vector2 потокозащищёнными!
        currentPosition.AddVector2(currentMoveVector2, currentSpeed)
    }

    override fun Print(iteration: Int?) // счётчик итераций для нескольких потоков. Int? - nullable reference types(не знаю как он правильно называется в kotlin, но C# точно так)
    {
        println("🏃${fullName}${if (iteration != null) " ${iteration}" else ""}: ${currentPosition.ToString()}")
    }
}