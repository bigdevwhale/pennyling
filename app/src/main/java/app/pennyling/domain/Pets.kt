package app.pennyling.domain

import app.pennyling.data.Pet

/**
 * A kind of pet. Keepers look after a spending category ([cat] is its built-in key) and hatch from its first records;
 * the others are found by living with money a certain way ([find]). [perk] is what it does in the team.
 */
data class Species(
    val id: String, val ru: String, val en: String, val color: Long,
    val cat: String = "",
    val names: List<String>, val namesEn: List<String>,
    val perkRu: String, val perkEn: String,
    val findRu: String = "", val findEn: String = "",
    val rare: Boolean = false,
) {
    val title get() = tr(ru, en)
    /** A keeper for a category of the player's own, chosen when the category is made. */
    val own get() = cat == Pets.OWN
    val perk get() = tr(perkRu, perkEn)
    val find get() = tr(findRu, findEn)
}

enum class Stage { EGG, BABY, TEEN, ADULT, MASTER }

/** Team perks: what doubles while this pet is in the team. */
enum class Perk { KEEPER, SNOW, WORTH, WISH, GOAL, DEBT, BILL, REVIEW, DAYS, HEARD, LONG, STASH, ALL }

object Pets {
    /** [Species.cat] of the keepers for the player's own categories. */
    const val OWN = "own"
    const val HATCH = 9
    val TEEN = 60; val ADULT = 260; val MASTER = 900

    private fun k(id: String, ru: String, en: String, color: Long, cat: String, names: String, namesEn: String, perkRu: String, perkEn: String) =
        Species(id, ru, en, color, cat, names.split(","), namesEn.split(","), perkRu, perkEn,
            "Первая запись в категории – и в норке появится его яйцо", "Its egg appears with the first record in its category")

    /** A keeper for a category the player makes: chosen for it, hatches from its records. */
    private fun o(id: String, ru: String, en: String, color: Long, names: String, namesEn: String, perkRu: String, perkEn: String) =
        Species(id, ru, en, color, OWN, names.split(","), namesEn.split(","), perkRu, perkEn,
            "Выберите его хранителем своей категории – яйцо появится с первой записью в ней",
            "Choose it as the keeper of a category of your own – its egg appears with the first record there")

    /** Keepers for the player's own categories ten to choose from. */
    val own: List<Species> = listOf(
        o("frog", "Лягушка", "Frog", 0xFF5FA04A, "Кувшинка,Квакша,Листик,Прыгун,Росинка", "Lily,Croaky,Leaf,Hopper,Dewdrop",
            "Хранитель вашей категории: в дождь и в вёдро бережёт каждую запись", "Keeper of a category of yours: minds every record, rain or shine"),
        o("stoat", "Горностай", "Stoat", 0xFFEDEAE2, "Снежок,Пёрышко,Писарь,Кисточка,Зимка", "Snowy,Quill,Scribe,Tip,Frost",
            "Хранитель вашей категории: аккуратно вносит каждую трату в книгу", "Keeper of a category of yours: writes every spend neatly in the ledger"),
        o("lynx", "Рысёнок", "Lynx kitten", 0xFFC89A62, "Кисточка,Пятнышко,Клубок,Рыська,Мурлыка", "Tufts,Spots,Yarn,Lynxie,Purr",
            "Хранитель вашей категории: зорко замечает, куда утекают деньги", "Keeper of a category of yours: sharp-eyed about where money slips away"),
        o("bear", "Медвежонок", "Bear cub", 0xFF8A5A36, "Медок,Топтыжка,Бочонок,Мишутка,Малинка", "Honey,Stomper,Barrel,Teddy,Berry",
            "Хранитель вашей категории: основательный и надёжный, как бочонок мёда", "Keeper of a category of yours: steady and solid, like a pot of honey"),
        o("fawn", "Оленёнок", "Fawn", 0xFFB97A48, "Ромашка,Крапинка,Копытце,Лужок,Пятнистик", "Daisy,Dapple,Hoof,Meadow,Speckle",
            "Хранитель вашей категории: бережно относится к каждой мелочи", "Keeper of a category of yours: gentle with every little thing"),
        o("snail", "Улитка", "Snail", 0xFFD9B98A, "Чашечка,Неспешка,Завиток,Ракушка,Чаёк", "Teacup,Slowpoke,Swirl,Shell,Chai",
            "Хранитель вашей категории: не торопится и не тратит зря", "Keeper of a category of yours: never rushes, never wastes"),
        o("robin", "Зарянка", "Robin", 0xFFE0703A, "Заря,Письмецо,Грудка,Пичуга,Огонёк", "Dawn,Letter,Redbreast,Chirp,Spark",
            "Хранитель вашей категории: приносит вести о каждой трате", "Keeper of a category of yours: brings word of every spend"),
        o("wolf", "Волчонок", "Wolf pup", 0xFF8A8F98, "Серый,Луна,Вой,Шарфик,Туман", "Grey,Moon,Howl,Scarf,Mist",
            "Хранитель вашей категории: верен своей стае и своему бюджету", "Keeper of a category of yours: loyal to the pack and the budget"),
        o("capybara", "Капибара", "Capybara", 0xFFA67C52, "Мандаринка,Тихоня,Ванна,Капа,Спокойствие", "Tangerine,Calm,Bath,Cappy,Zen",
            "Хранитель вашей категории: невозмутимо держит траты в узде", "Keeper of a category of yours: keeps spending in check without a fuss"),
        o("redpanda", "Малая панда", "Red panda", 0xFFC0502A, "Блинчик,Колечко,Рыжик,Пандочка,Бамбук", "Pancake,Ring,Ginger,Panda,Bamboo",
            "Хранитель вашей категории: весёлый, но счёт деньгам знает", "Keeper of a category of yours: cheerful, but counts every coin"),
    )
    private val base: List<Species> = listOf(
        Species("hamster", "Хомяк", "Hamster", 0xFFE0A050, "other", "Пшёнка,Щёкин,Зёрнышко,Бублик,Сырник".split(","), "Millet,Cheeks,Nugget,Bagel,Biscuit".split(","),
            "Главный по кладовой. Что осталось от пайка, прячет за щёку – в конце месяца покажет, сколько набралось",
            "Head of the pantry. What's left of the daily ration he tucks in his cheek and shows it all at the end of the month",
            "Живёт в норке с первого дня", "Lives in the burrow from day one"),
        k("squirrel", "Белка", "Squirrel", 0xFFD2692E, "food", "Рыжик,Шишка,Орешек,Векша,Хвостуша", "Rusty,Cone,Hazel,Fluff,Pip",
            "Отвечает за продукты: первой замечает, когда они дорожают", "Keeps the groceries: notices when the weekly basket gets heavier"),
        k("raccoon", "Енот", "Raccoon", 0xFF7C7C86, "cafe", "Мокко,Пенка,Бариста,Латте,Полоскун", "Mocha,Foam,Barista,Latte,Bandit",
            "Отвечает за кафе и доставку: знает, во что обходятся перекусы на бегу", "Keeps cafés and delivery: counts what «a quick bite» costs"),
        k("beaver", "Бобр", "Beaver", 0xFF8B5A2B, "home", "Плотник,Бревно,Строгаль,Запруда,Тёс", "Plank,Log,Chip,Dam,Timber",
            "Отвечает за дом и коммуналку, а ещё строит плотину – подушку безопасности", "Keeps home and bills, and builds the dam – your safety cushion"),
        k("mole", "Крот", "Mole", 0xFF4B3B47, "transport", "Тоннель,Билетик,Подземкин,Фонарик,Норушкин", "Tunnel,Ticket,Digby,Lantern,Burrows",
            "Отвечает за транспорт: знает цену каждой поездки", "Keeps transport: knows what each trip costs"),
        k("hedgehog", "Ёж", "Hedgehog", 0xFF9A6A48, "health", "Колючка,Яблочко,Пых,Иголкин,Ёжик", "Prickles,Apple,Puff,Needles,Spike",
            "Отвечает за здоровье: аптеку, врачей, спорт", "Keeps health: pharmacy, doctors, sport"),
        k("magpie", "Сорока", "Magpie", 0xFF2E3A4E, "shopping", "Блестяшка,Трещотка,Сорочка,Колечко,Белобока", "Sparkle,Chatter,Trinket,Ring,Glint",
            "Отвечает за покупки и одежду, а ещё замечает повторяющиеся платежи", "Keeps shopping and clothes, and sniffs out repeating payments"),
        k("chipmunk", "Бурундук", "Chipmunk", 0xFFC08040, "fun", "Полоска,Фокусник,Хохотун,Жонглёр,Бурунчик", "Stripes,Trickster,Giggles,Juggler,Chip",
            "Отвечает за развлечения: кино, игры, концерты", "Keeps fun: films, games, concerts"),
        k("bat", "Летучая мышь", "Bat", 0xFF6B4A7A, "connect", "Сигнал,Антенна,Вайфай,Нетопырь,Эхо", "Signal,Antenna,Wifi,Echo,Ping",
            "Отвечает за связь и подписки и присматривает за погребом", "Keeps phone, internet and subscriptions; minds the cellar of barrels"),
        k("mouse", "Мышка", "Mouse", 0xFFB8A08A, "family", "Норушка,Шалька,Пуговка,Мышута,Клубочек", "Button,Shawl,Pip,Tilly,Yarn",
            "Отвечает за семью и детей", "Keeps family and kids"),
        k("bee", "Пчела", "Bee", 0xFFE8B030, "gifts", "Медуница,Жужа,Бантик,Сота,Пыльца", "Honey,Buzz,Bow,Comb,Pollen",
            "Отвечает за подарки и праздники", "Keeps gifts and celebrations"),
        k("pika", "Пищуха", "Pika", 0xFF9C9488, "travel", "Тропка,Карта,Перевал,Странник,Компас", "Trail,Map,Pass,Rover,Compass",
            "Отвечает за путешествия", "Keeps travel"),
        k("rabbit", "Кролик", "Rabbit", 0xFFE8DCDC, "care", "Пушок,Тюрбан,Ушастик,Зефир,Снежок", "Fluff,Turban,Ears,Marshmallow,Snowy",
            "Отвечает за уход за собой: стрижки, косметику, спа", "Keeps self-care: beauty, haircuts, grooming"),
        k("owl", "Сова", "Owl", 0xFF8A6A4A, "learn", "Совушка,Очкарик,Букварь,Полночь,Мудрик", "Hoot,Specs,Primer,Midnight,Sage",
            "Отвечает за учёбу и книги, а по понедельникам помогает разобрать прошлую неделю", "Keeps learning and books, and on Mondays goes over last week with you"),
        k("ferret", "Хорёк", "Ferret", 0xFFB07848, "hobby", "Кисточка,Мольберт,Клубок,Умелец,Хорь", "Brush,Easel,Yarn,Tinker,Doodle",
            "Отвечает за хобби: краски, пряжу, инструменты, всё, чем заняты руки", "Keeps hobbies: paints, yarn, tools, whatever keeps your hands busy"),
        Species("ant", "Муравей", "Ant", 0xFFC0392B, "", "Трудяга,Крошка,Мураш,Носильщик,Капелька".split(","), "Toiler,Crumb,Antsy,Porter,Drop".split(","),
            "Округляет каждую трату вверх и показывает, сколько набралось бы на сдаче",
            "Rounds every spend up and puts the difference aside – you see what a jar would collect",
            "Записывайте траты 7 дней, можно не подряд", "Log money on 7 different days"),
        Species("jay", "Сойка", "Jay", 0xFF5B8FD0, "", "Синька,Желудёк,Сойкин,Пёрышко,Прятка".split(","), "Bluey,Acorn,Jaybird,Feather,Hider".split(","),
            "Отмечает снежные дни – дни совсем без трат", "Marks snow days – days with no spending at all",
            "Отметьте первый день без трат", "Mark your first day without spending"),
        Species("dormouse", "Соня", "Dormouse", 0xFFE08A3A, "", "Дрёма,Колпачок,Сплюшка,Орешек,Засоня".split(","), "Dozy,Nightcap,Snooze,Hazel,Sleepy".split(","),
            "Сладко спит в снежные дни, и каждый такой день приносит вдвое больше", "Sleeps sweetly on snow days and doubles each one",
            "Пять снежных дней", "Five snow days"),
        Species("otter", "Выдра", "Otter", 0xFF7A5A3E, "", "Камешек,Волна,Плюх,Речка,Гладыш".split(","), "Pebble,Wave,Splash,Brook,Sleek".split(","),
            "Спрашивает, стоила ли покупка своих денег, и считает «радость на рубль»", "Asks whether it was worth it and collects «joy per coin»",
            "Ответьте на 15 вопросов «Стоило того?»", "Answer 15 «Worth it?» questions"),
        Species("fox", "Лиса", "Fox", 0xFFE0662A, "", "Хитрюга,Фонарик,Рыжуля,Плутовка,Огонёк".split(","), "Sly,Lantern,Ginger,Trickster,Ember".split(","),
            "Сторожит чердак желаний: учит подождать, прежде чем покупать", "Guards the attic of wishes: teaches you to wait before you buy",
            "Решите судьбу первого желания с чердака", "Decide on your first wish from the attic"),
        Species("badger", "Барсук", "Badger", 0xFF505860, "", "Фонарь,Камнекат,Барсучок,Шахтёр,Полоска".split(","), "Lamp,Roller,Brock,Miner,Stripe".split(","),
            "Откатывает валуны долгов от входа, платёж за платежом", "Rolls the boulders of debt away from the door, payment by payment",
            "Внесите первый платёж по долгу", "Make your first debt payment"),
        Species("crow", "Ворона", "Crow", 0xFF2A2A34, "", "Ключик,Монокль,Каркуша,Вестник,Уголёк".split(","), "Key,Monocle,Cawley,Herald,Coal".split(","),
            "Приносит вести из банка: уведомления превращаются в готовые записи", "Brings news from the bank: notifications turn into ready records",
            "Примите первую запись от вороны или проверьте 3 подписки", "Take the crow's first record or review 3 subscriptions"),
        Species("marmot", "Сурок", "Marmot", 0xFFA07850, "", "Дозорный,Подзорка,Свистун,Сурчик,Пригорок".split(","), "Lookout,Spyglass,Whistler,Woody,Knoll".split(","),
            "Смотрит вперёд: подскажет, хватит ли запаса до зарплаты", "Looks ahead: will the pantry last until payday",
            "Проживите с Pennyling первый месяц до зарплаты", "Live through your first winter – the first month to payday"),
        Species("tortoise", "Черепаха", "Tortoise", 0xFF6A8A4A, "", "Копилка,Тихоня,Панцирь,Неспешка,Мох".split(","), "Piggy,Steady,Shell,Slowpoke,Moss".split(","),
            "Отвечает за долгие цели: медленно, но всегда вперёд", "Keeps long goals: slowly, but always onward",
            "Записывайте траты 60 дней", "Log money on 60 different days"),
        Species("pangolin", "Панголин", "Pangolin", 0xFFD4A82E, "", "Червонец,Чешуйка,Золотко,Дублон,Клад".split(","), "Ducat,Scale,Goldie,Doubloon,Hoard".split(","),
            "Легенда кладовой. С ним все крошки в норке приходят с прибавкой", "Legend of the pantry. With him, everything the burrow gives brings more",
            "Шесть месяцев подряд укладывайтесь в бюджет", "Stay within budget for six months", rare = true),
    )
    val all: List<Species> = base + own
    val byId = all.associateBy { it.id }
    fun of(id: String) = byId[id] ?: all[0]
    fun keeperOf(catKey: String): Species? = all.firstOrNull { it.cat == catKey && catKey.isNotEmpty() }

    /** What each pet doubles while it's in the team. */
    fun perkOf(id: String): Perk = when (id) {
        "jay", "dormouse" -> Perk.SNOW; "otter" -> Perk.WORTH; "fox" -> Perk.WISH; "tortoise" -> Perk.GOAL; "badger" -> Perk.DEBT
        "bat" -> Perk.BILL; "owl" -> Perk.REVIEW; "ant" -> Perk.DAYS; "crow" -> Perk.HEARD; "marmot" -> Perk.LONG; "hamster" -> Perk.STASH
        "pangolin" -> Perk.ALL; else -> Perk.KEEPER
    }
}

fun Pet.stage(): Stage = when {
    hatched == 0L -> Stage.EGG
    xp < Pets.TEEN -> Stage.BABY
    xp < Pets.ADULT -> Stage.TEEN
    xp < Pets.MASTER -> Stage.ADULT
    else -> Stage.MASTER
}

fun Stage.title() = when (this) {
    Stage.EGG -> tr("Яйцо", "Egg"); Stage.BABY -> tr("Малыш", "Baby"); Stage.TEEN -> tr("Подросток", "Youngster")
    Stage.ADULT -> tr("Взрослый", "Grown-up"); Stage.MASTER -> tr("Старейшина", "Elder")
}

/** XP to the next stage, as (have, need) within the stage. */
fun Pet.toNext(): Pair<Int, Int> = when (stage()) {
    Stage.EGG -> growth to Pets.HATCH
    Stage.BABY -> xp to Pets.TEEN
    Stage.TEEN -> (xp - Pets.TEEN) to (Pets.ADULT - Pets.TEEN)
    Stage.ADULT -> (xp - Pets.ADULT) to (Pets.MASTER - Pets.ADULT)
    Stage.MASTER -> 1 to 1
}
