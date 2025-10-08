import java.util.concurrent.locks.ReentrantLock
// мьютекс из java (kotlinx.coroutines.sync.Mutex для корутин котлина, я буду использовать java потоки. Как будто так привычней, без suspend функций)

class Point(_x: Float, _y: Float) // эти классы тоже сделаю потокозащищёнными!
{
    val mutex: ReentrantLock = ReentrantLock() // для каждого экземпляра будет свой мьютекс

    var x: Float = _x
        set(value)
        {
            mutex.lock()
            try { field = value }
            finally { mutex.unlock() }
        }
    var y: Float = _y
        set(value)
        {
            mutex.lock()
            try { field = value }
            finally { mutex.unlock() }
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