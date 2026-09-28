package dev.kiritoxd.miaopu.data

enum class Esport(
    val businessId: String,
    val title: String,
    val shortTitle: String,
    val defaultSubscribed: Boolean,
    val category: ScheduleCategory = ScheduleCategory.ESPORTS,
    val supplementalSources: List<ScheduleSource> = emptyList(),
    val primaryScheduleBusinessId: String? = businessId,
) {
    LOL("lol", "英雄联盟", "LOL", true),
    KOG("kog", "王者荣耀", "KPL", true),
    CS2("cs2", "反恐精英 2", "CS2", true),
    VALORANT("val", "无畏契约", "VAL", true),
    PUBG("pubg", "绝地求生", "PUBG", true),
    NBA(
        businessId = "nba",
        title = "NBA",
        shortTitle = "NBA",
        defaultSubscribed = false,
        category = ScheduleCategory.BASKETBALL,
        supplementalSources = listOf(ScheduleSource.common("(^|[^A-Z])NBA([^A-Z]|$)")),
    ),
    CBA(
        businessId = "cba",
        title = "CBA / 中国篮球",
        shortTitle = "CBA",
        defaultSubscribed = false,
        category = ScheduleCategory.BASKETBALL,
        supplementalSources = listOf(
            ScheduleSource.unfiltered("chinabasketball"),
            ScheduleSource.common("(^|[^A-Z])CBA([^A-Z]|$)|中国篮球|男篮|女篮"),
        ),
    ),
    WNBA(
        businessId = "wnba",
        title = "WNBA",
        shortTitle = "WNBA",
        defaultSubscribed = false,
        category = ScheduleCategory.BASKETBALL,
        supplementalSources = listOf(ScheduleSource.common("WNBA")),
    ),
    CUBAL(
        businessId = "cuba",
        title = "CUBAL",
        shortTitle = "CUBAL",
        defaultSubscribed = false,
        category = ScheduleCategory.BASKETBALL,
        supplementalSources = listOf(ScheduleSource.common("CUBAL|CUBA")),
    ),
    ENGLISH_PREMIER_LEAGUE(
        businessId = "epl",
        title = "英格兰足球超级联赛",
        shortTitle = "英超",
        defaultSubscribed = false,
        category = ScheduleCategory.FOOTBALL,
        supplementalSources = listOf(ScheduleSource.common("英超|Premier League")),
    ),
    WORLD_CUP(
        businessId = "worldcup",
        title = "足球世界杯",
        shortTitle = "世界杯",
        defaultSubscribed = false,
        category = ScheduleCategory.FOOTBALL,
        supplementalSources = listOf(ScheduleSource.common("足球世界杯|(?<!篮)世界杯")),
    ),
    FOOTBALL(
        businessId = "football",
        title = "其他足球",
        shortTitle = "足球",
        defaultSubscribed = false,
        category = ScheduleCategory.FOOTBALL,
        supplementalSources = listOf(
            ScheduleSource.common(
                "^(?!.*(?:英超|世界杯)).*足球.*$|德国杯|足总杯|欧冠|欧联|世俱杯|西甲|意甲|法甲|中超",
            ),
        ),
        primaryScheduleBusinessId = null,
    ),
    TENNIS(
        "tennis",
        "网球",
        "网球",
        false,
        ScheduleCategory.SPORTS,
        listOf(ScheduleSource.common("网球|ATP|WTA|美网|澳网|法网|温网")),
    ),
    TABLE_TENNIS(
        "tabletennis",
        "乒乓球",
        "乒乓球",
        false,
        ScheduleCategory.SPORTS,
        listOf(ScheduleSource.common("乒乓|WTT|世乒|乒超")),
    ),
    BADMINTON(
        "badminton",
        "羽毛球",
        "羽球",
        false,
        ScheduleCategory.SPORTS,
        listOf(ScheduleSource.common("羽毛球|BWF|苏迪曼|汤姆斯|尤伯|汤尤杯")),
    ),
    FORMULA_ONE(
        businessId = "formula_one",
        title = "一级方程式",
        shortTitle = "F1",
        defaultSubscribed = false,
        category = ScheduleCategory.SPORTS,
        supplementalSources = listOf(ScheduleSource.common("F1|一级方程式")),
        primaryScheduleBusinessId = null,
    ),
    SNOOKER(
        businessId = "snooker",
        title = "斯诺克",
        shortTitle = "斯诺克",
        defaultSubscribed = false,
        category = ScheduleCategory.SPORTS,
        supplementalSources = listOf(ScheduleSource.common("斯诺克|台球")),
        primaryScheduleBusinessId = null,
    ),
    VOLLEYBALL(
        businessId = "volleyball",
        title = "排球",
        shortTitle = "排球",
        defaultSubscribed = false,
        category = ScheduleCategory.SPORTS,
        supplementalSources = listOf(ScheduleSource.common("排球|女排|男排|VNL")),
        primaryScheduleBusinessId = null,
    ),
}

enum class ScheduleCategory(val title: String) {
    ESPORTS("电竞赛事"),
    BASKETBALL("篮球赛事"),
    FOOTBALL("足球赛事"),
    SPORTS("其他体育"),
}

class ScheduleSource private constructor(
    val businessId: String,
    introductionPattern: String,
) {
    private val introductionRegex = Regex(introductionPattern, RegexOption.IGNORE_CASE)

    fun includes(introduction: String): Boolean = introductionRegex.containsMatchIn(introduction)

    companion object {
        internal const val COMMON_HOT_SPORTS_BUSINESS_ID = "commonhotsports"

        fun common(introductionPattern: String): ScheduleSource =
            ScheduleSource(COMMON_HOT_SPORTS_BUSINESS_ID, introductionPattern)

        fun unfiltered(businessId: String): ScheduleSource = ScheduleSource(businessId, ".*")
    }
}

object EsportCatalog {
    val all: List<Esport> = Esport.entries

    fun subscriptions(savedBusinessIds: Set<String>?): Set<Esport> {
        if (savedBusinessIds == null) return all.filterTo(linkedSetOf()) { it.defaultSubscribed }
        val resolved = all.filterTo(linkedSetOf()) { it.businessId in savedBusinessIds }
        return resolved.ifEmpty { linkedSetOf(Esport.LOL) }
    }

    fun orderedSubscriptions(savedBusinessIds: Set<String>?, savedOrder: List<String>): List<Esport> {
        val subscribed = subscriptions(savedBusinessIds)
        return (savedOrder.mapNotNull(::byBusinessId).filter { it in subscribed } + subscribed).distinct()
    }

    fun byBusinessId(id: String?): Esport? = all.firstOrNull { it.businessId == id }
}
