package uno.lux.autoskola.practice

data class Question(
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val points: Int,
)

val sampleQuestions: List<Question> = listOf(
    Question(
        text = "Vozidlo sa považuje za technicky nespôsobilé na premávku na pozemných komunikáciách, ak",
        options = listOf(
            "sú prekročené emisné limity motora určené výrobcom; ak ich výrobca neučil, tak ustanovené emisné limity motora,",
            "je katalyzátor výfukových plynov po lehote kalibrácie,",
            "nieje vybavené riadeným katalyzátorom.",
        ),
        correctIndex = 0,
        points = 2,
    ),
    Question(
        text = "Vodič motorového vozidla je počas jazdy povinný",
        options = listOf(
            "byť pripútaný bezpečnostným pásom, ak ním je vozidlo vybavené,",
            "mať otvorené okno na strane vodiča,",
            "mať zapnuté výstražné svetelné zariadenie.",
        ),
        correctIndex = 0,
        points = 2,
    ),
    Question(
        text = "Účastník dopravnej nehody je povinný",
        options = listOf(
            "vystúpiť z vozidla a vzdialiť sa od miesta nehody,",
            "okamžite zastaviť vozidlo a urobiť potrebné opatrenia na záchranu osôb a majetku,",
            "okamžite odísť z miesta nehody a privolať pomoc telefonicky.",
        ),
        correctIndex = 1,
        points = 3,
    ),
)
