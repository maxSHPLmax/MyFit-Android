package com.maxshpl.myfit.reminders

data class ReminderSlot(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
) {
    companion object {
        fun defaultFor(kind: MealKind): ReminderSlot = ReminderSlot(
            enabled = true,
            hour = kind.defaultHour,
            minute = kind.defaultMinute,
        )
    }
}

data class RemindersConfig(
    val enabled: Boolean,
    val breakfast: ReminderSlot,
    val lunch: ReminderSlot,
    val dinner: ReminderSlot,
    val snack: ReminderSlot,
) {
    fun slotFor(kind: MealKind): ReminderSlot = when (kind) {
        MealKind.BREAKFAST -> breakfast
        MealKind.LUNCH -> lunch
        MealKind.DINNER -> dinner
        MealKind.SNACK -> snack
    }

    fun withSlot(kind: MealKind, slot: ReminderSlot): RemindersConfig = when (kind) {
        MealKind.BREAKFAST -> copy(breakfast = slot)
        MealKind.LUNCH -> copy(lunch = slot)
        MealKind.DINNER -> copy(dinner = slot)
        MealKind.SNACK -> copy(snack = slot)
    }

    fun activeSlots(): List<Pair<MealKind, ReminderSlot>> = MealKind.entries
        .map { it to slotFor(it) }
        .filter { (_, slot) -> slot.enabled }

    companion object {
        // Все 4 слота ON дефолтно — discoverability (см. ответ Lead на вопрос 4).
        val Default = RemindersConfig(
            enabled = false,
            breakfast = ReminderSlot.defaultFor(MealKind.BREAKFAST),
            lunch = ReminderSlot.defaultFor(MealKind.LUNCH),
            dinner = ReminderSlot.defaultFor(MealKind.DINNER),
            snack = ReminderSlot.defaultFor(MealKind.SNACK),
        )
    }
}
