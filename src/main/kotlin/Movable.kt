interface Movable
{
    val currentPosition: Point
    val currentMoveVector2: Vector2
    val currentSpeed: Float

    fun RandomizeMoveVector()
    fun Move()
    fun Print(iteration: Int? = null)  // TODO: не забыть убрать iteration в будущем
}