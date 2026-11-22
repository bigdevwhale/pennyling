# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.6.0] - 2026-11-22

### Added
- **A snow day can be marked afterwards (#18):** the burrow offers to mark yesterday if nothing was spent and it wasn't marked; any earlier day without spending is marked (or unmarked) from the journal's calendar. It pays crumbs and counts for the week's tasks like one marked on the day.

<!-- tg
Снежный день можно отметить задним числом: вчерашний – прямо в Норке, любой прошлый день без трат – в календаре Журнала
-->

## [1.5.0] - 2026-11-20

### Added
- **Ten keepers for categories of your own:** a frog, a stoat, a lynx kitten, a bear cub, a fawn, a snail, a robin, a wolf pup, a capybara and a red panda. Pick one when you make a category or edit it: its egg comes with the category's first record and hatches from its records. They don't turn up in jars or at the fair.
- **A new category right from a record:** the "+ New" tile after the categories, with a keeper to choose.

### Changed
- **Spending from a savings pot leaves the daily ration alone:** a big one-off from an account of the "Savings" kind no longer sinks the day's ration (#17, #12).
- **The record form:** the account, the day and "Where, what, why" come right under the amount, the categories after them (#9).
- **Names under the category icons** in every category picker: the crow's records and regular payments too (#13).

### Fixed
- The day opened under the calendar now scrolls and shows all its records (#16).
- The keyboard no longer covers "Where, what, why": the page moves up and the amount keypad steps aside while typing (#14).

<!-- tg
Для своих категорий – десять новых хранителей: лягушка, горностай, рысёнок, медвежонок, оленёнок, улитка, зарянка, волчонок, капибара и малая панда
Новую категорию можно завести прямо из записи, а траты с копилки больше не съедают дневной паёк
В записи сначала счёт и «Где, что, зачем», под иконками категорий – подписи
Исправлено: в календаре видны все траты дня, клавиатура не закрывает поле заметки
-->

## [1.4.0] - 2026-11-18

### Added
- **Moving to a new phone through Lingvale:** Lingvale can now gather the backups of all our apps into one file and load them back on another phone. Pennyling hands it the same backup file the settings save and takes one back the same way. Only Lingvale signed with our key is answered, loading happens only after you confirm it in Lingvale, and nothing leaves the phone.

<!-- tg
Переезд на новый телефон через Lingvale: деревня собирает копии всех наших приложений в один файл и возвращает их на новом телефоне.
Pennyling отдаёт туда ту же копию, что сохраняют настройки.
-->

## [1.3.1] - 2026-11-17

### Fixed
- **Each egg hatches from its own category:** a keeper's egg now grows only on records of its category, so a transport record no longer hatches the chipmunk of fun. Eggs found for habits and the surprise eggs from a full jar or the fair still warm on every record (#8).

<!-- tg
Яйца в норке теперь вылупляются от записей своей категории: трата на транспорт больше не выведет бурундука за развлечения.
Яйца-сюрпризы из банки и с ярмарки, как и раньше, греются от любых записей.
-->

## [1.3.0] - 2026-11-16

### Added
- **Lingvale, the village of the pets from all our apps:** Pennyling now has a small read-only bridge, so in Lingvale you can build the burrow and its residents move in with their days. The bridge answers only Lingvale signed with our key; it changes nothing and nothing leaves the phone.

<!-- tg
Питомцы Pennyling теперь могут жить в Lingvale – общей деревне наших приложений.
В Lingvale для них можно построить норку, и жильцы переедут туда сами вместе со своими днями.
Данные остаются в Pennyling: деревня их только читает.
-->

## [1.2.1] - 2026-11-16

### Added
- The owl's week in review: a category opens into its records of the week, biggest first, each with its day; "All categories" shows the ones past the top four.

<!-- tg
Новое: в разборе недели можно нажать на категорию и увидеть, из каких трат сложилась сумма. А кнопка «Все категории» покажет не только первые четыре.
-->

## [1.2.0] - 2026-11-16

### Added
- A built-in "Hobbies" spending category with its own keeper, the ferret: its egg turns up with the first record, like every other keeper's (#2). A burrow set up before gets it after "Fun"; a category of your own called "Hobbies" becomes it and keeps its records.
- Categories change places with a long press and a drag, in the category grid of a new record and on the Categories page (#4).

<!-- tg
Новое: категория «Хобби» со своим хранителем – хорьком. Его яйцо появится с первой же записью. Если у вас уже была своя категория «Хобби», она станет этой, записи сохранятся.
Новое: категории можно менять местами – удерживайте одну и перетащите. Работает и на экране записи, и в разделе «Категории».
-->

## [1.1.0] - 2026-11-13

### Added
- A regular payment can repeat on any period: "Custom" sets every N days, weeks, months or years, so a subscription billed every two months fits (#1). The cellar's yearly sum counts it right.

### Fixed
- The "How often" choices wrap onto a second line instead of running off the edge of narrow screens (#1).

<!-- tg
Новое: у регулярного платежа можно задать свой период – раз в сколько угодно дней, недель, месяцев или лет. Например, подписка раз в два месяца.
Исправлено: кнопка «Каждый год» больше не уезжает за край экрана.
-->

## [1.0.0] - 2026-11-11

The first release of Pennyling.

- Records on a big keypad: spending, income and transfers, categories with their keepers, «the usual» in one tap, accounts (cash, card, savings), any day, notes with suggestions.
- The month from payday to payday as a winter: the pantry is the budget, today's ration is what's left divided by the days to payday, with bills still to come kept aside; snow falls outside the burrow window as the month runs out. Snow days for days with no spending.
- 24 pets: 14 category keepers that hatch from the first records in their category and 10 found by good habits, including the legendary pangolin. A team of three doubles crumbs for its own thing; five stages; furniture and surprise eggs at the fair.
- The pantry: jars for goals (a full jar holds an egg), the attic where wishes cool before buying, the cellar of subscriptions and regular payments with reviews and repeating payments found by the magpie, boulders of debt with snowball and avalanche plans, the beaver's dam for the safety cushion.
- «Was it worth it?» a week after a spend; the crow turns bank notifications into ready records; weekly tasks and the owl's review of the week; an evening reminder.
- Statistics in four tabs: the forecast to payday, categories with limits, month by month, a spending calendar, weekdays and hours, joy per coin, little things, favourite places, cost in hours of work, subscriptions a year, personal inflation, net worth, a «what if» calculator and a year in the burrow.
- Six kinds of share cards, a backup file, light and dark themes, Russian and English, 29 currencies, an introduction on every screen.

<!-- tg
Pennyling – учёт денег в уютной норке. Месяц до зарплаты – это зима: бюджет лежит в кладовой, каждый день у вас свой паёк, а к концу месяца за окном выпадает снег.
24 питомца: у каждой категории трат свой хранитель, ещё десять появляются за полезные привычки. Банки для целей, чердак для желаний, погреб подписок, долги и подушка безопасности.
Много статистики, вопрос «Стоило того?» через неделю после покупки и ворона, которая превращает уведомления банка в готовые записи.
-->
