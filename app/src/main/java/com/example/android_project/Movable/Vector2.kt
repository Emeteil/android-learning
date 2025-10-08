import java.util.concurrent.locks.ReentrantLock
// мьютекс из java (kotlinx.coroutines.sync.Mutex для корутин котлина, я буду использовать java потоки. Как будто так привычней, без suspend функций)

import kotlin.math.pow
import kotlin.math.sqrt

// нормализованный
class Vector2(_x: Float, _y: Float)
{
    protected val mutex = ReentrantLock()

    var x: Float = 0f
        set(value)
        {
            mutex.lock()
            try { field = value }
            finally { mutex.unlock() }
        }
    var y: Float = 0f
        set(value)
        {
            mutex.lock()
            try { field = value }
            finally { mutex.unlock() }
        }

    init
    {
        var d = sqrt(_x.pow(2) + y.pow(2))
        if (d != 0f)
        {
            this.x = _x / d
            this.y = _y / d
        }
        else
        {
            this.x = _x
            this.y = _y
        }
    }

    fun ToString(): String
    {
        return "(${x}, ${y})"
    }
}