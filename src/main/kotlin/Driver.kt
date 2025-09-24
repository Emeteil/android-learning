class Driver(name: String, startSpeed: Float, moveVector2: Vector2) : Human(name, 0, 0, startSpeed)
{
    init
    {
        currentMoveVector2 = moveVector2 // так как у меня всё движение построенно на векторе движения, то функцию move переопределять необязательно
    }

    override fun RandomizeMoveVector() {} // движение прямолинейно - рандомизация не нужна (TODO: возможно сделать небольшую рандомизацию скорости)

    override fun Print(iteration: Int?)
    {
        println("🛻${fullName}${if (iteration != null) " ${iteration}" else ""}: ${currentPosition.ToString()}")
    }
}