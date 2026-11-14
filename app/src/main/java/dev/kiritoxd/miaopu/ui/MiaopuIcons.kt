package dev.kiritoxd.miaopu.ui

import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.glass.Add
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.glass.ChevronForward
import top.yukonga.miuix.kmp.icon.glass.Close
import top.yukonga.miuix.kmp.icon.glass.ContactsCircle
import top.yukonga.miuix.kmp.icon.glass.Date
import top.yukonga.miuix.kmp.icon.glass.HomeOutline
import top.yukonga.miuix.kmp.icon.glass.Refresh
import top.yukonga.miuix.kmp.icon.glass.Search
import top.yukonga.miuix.kmp.icon.glass.StarFill
import top.yukonga.miuix.kmp.icon.glass.StarOutline

// Miuix's OS4 icon set consists of static vectors; it does not enable a glass effect.
internal object MiaopuIcons {
    val CalendarDays get() = MiuixIcons.Glass.Date
    val Check get() = MiuixIcons.Basic.Check
    val ChevronLeft get() = MiuixIcons.Glass.ChevronBackward
    val ChevronRight get() = MiuixIcons.Glass.ChevronForward
    val CircleUserRound get() = MiuixIcons.Glass.ContactsCircle
    val House get() = MiuixIcons.Glass.HomeOutline
    val Plus get() = MiuixIcons.Glass.Add
    val RefreshCw get() = MiuixIcons.Glass.Refresh
    val Search get() = MiuixIcons.Glass.Search
    val Star get() = MiuixIcons.Glass.StarOutline
    val StarFilled get() = MiuixIcons.Glass.StarFill
    val X get() = MiuixIcons.Glass.Close
}
