fun main()
{
    val THREAD_COUNT = 4 // я решил сделать ограничение количество потоков, в одном потоке до нескольких обработчиков движения (если понадобится можно сделать THREAD_COUNT = peoples.size)

    var peoples: Array<Human> = arrayOf(
        Human("Я", 19, 433, 1f),
        Human("Петя", 20, 123, 1f),
        Human("Ваня", 20, 867, 1f),
        Driver("Таксист Олег", 2f, Vector2(1f, 0f)), // для примера машина будет ехать в 2 раза быстрее человека и прямолинейно по оси x
    ) // TODO: сделать MutableList для изменяймости в будущем

    val threads: MutableList<Thread> = mutableListOf<Thread>()
    var isRunningThreads: Boolean = true

    for (i in 0 until THREAD_COUNT) {
        val thread = Thread {
            var it: Int = 0
            while (isRunningThreads) // для остановки потоков
            {
                for (j in i until peoples.size step THREAD_COUNT)
                {
                    peoples[j].RandomizeMoveVector()
                    peoples[j].Move()
                    it++

                    peoples[j].Print(it)
                    Thread.sleep(10) // задержка между каждым объектом отдельно (для медленного вывода в консоль, потом уберётся)
                }
                Thread.sleep(500) // задержка каждого потока (для медленного вывода в консоль, потом уберётся)
            }
        }
        threads.add(thread)
        thread.start()
    }

    while (true)
    {
        val input = readLine()
        if (input != null)
        {
            isRunningThreads = false
            break
        }
    }

    threads.forEach {
        it.join()
    }
}